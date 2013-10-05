#include <errno.h>
#include <stdio.h>
#include <dirent.h>
#include <asm/signal.h>

#ifdef ANDROID
#include <sys/system_properties.h>
#endif
#include "parser/stdapi.h"
#include "parser/parser.h"

/*
 * Macros
 */
#define PROPERTY_KEY_MAX        32
#define PROPERTY_VALUE_MAX      92

/* Token IDs*/
#define TID_COMMENT             1024
#define TID_RUN                 1025
#define TID_GETPROP             1026
#define TID_SETPROP             1027
#define TID_UIPRINT             1028
#define TID_UIPRINT_ACTION      1029
#define TID_KILLBYPIDFILE      	1030
#define TID_KILLBYNAME			1031

/* Command Format IDs */
#define FID_COMMENT             TID_COMMENT
#define FID_RUN                 TID_RUN
#define FID_GETPROP             TID_GETPROP
#define FID_SETPROP             TID_SETPROP
#define FID_UIPRINT             TID_UIPRINT
#define FID_UIPRINT_ACTION      TID_UIPRINT_ACTION
#define FID_KILLBYPIDFILE      	TID_KILLBYPIDFILE
#define FID_KILLBYNAME			TID_KILLBYNAME

#define FID_INCLUDE             2047

typedef struct
{
	/* Used for callback functions to set return info */
	int callbackReturnCode;
	void* callbackReturnData;
	FILE* cmd_pipe;
	FILE* log_fp;
} UpdaterInfo;

typedef void
(*ParserCallbackFn)(UpdaterInfo* pUI, TOKENINFO* pTokens, int nTokens,
		int nLineNo);

/*
 * Forward Declarations
 */
static void
RunFn(UpdaterInfo* pUI, TOKENINFO* pTokens, int nTokens, int nLineNo);
static void
GetPropFn(UpdaterInfo* pUI, TOKENINFO* pTokens, int nTokens, int nLineNo);
static void
SetPropFn(UpdaterInfo* pUI, TOKENINFO* pTokens, int nTokens, int nLineNo);
static void
UIPrintFn(UpdaterInfo* pUI, TOKENINFO* pTokens, int nTokens, int nLineNo);
static void
UIPrintAction(UpdaterInfo* pUI, TOKENINFO* pTokens, int nTokens,
		int nLineNo);

/*
 * Private variables
 */
static const int READ_BUF_SIZE = 50;

/* The token table */
static TOKENDEF TokenTable[] =
{
/* ---------------------------- Command Tokens ---------------------------- */
	{ TID_COMMENT, TT_PUNCT, FALSE, "#" },
	{ TID_RUN, TT_ALPHA, FALSE, "run" },
	{ TID_GETPROP, TT_ALPHA, FALSE, "getprop" },
	{ TID_SETPROP, TT_ALPHA, FALSE, "setprop" },
	{ TID_KILLBYPIDFILE, TT_ALPHA, FALSE, "killbypidfile" },
	{ TID_KILLBYNAME, TT_ALPHA, FALSE, "killbyname" },
	{ 0, 0, 0, 0 }
};

static FORMATDEF FormatTable[] =
{
/* ---------------------------- Command Formats ---------------------------- */
		/* Comments: # ... */
	{ FIT_PUNCT, FP_FORMAT_BEG, FID_COMMENT },
	{ FIT_TOKEN, TID_COMMENT, TT_PUNCT }, /* the # character */
	{ FIT_PUNCT, FP_OPTL_BEG, TT_NULL },
	{ FIT_PUNCT, FP_REPT_BEG, TT_NULL },
	{ FIT_TOKEN, TID_ANY, TT_ANY }, /* any number of tokens after the # */
	{ FIT_PUNCT, FP_REPT_END, TT_NULL },
	{ FIT_PUNCT, FP_OPTL_END, TT_NULL },
	{ FIT_PUNCT, FP_FORMAT_END, FID_COMMENT },

	/* run some_command [ arg1 arg2 ... argn ] */
	{ FIT_PUNCT, FP_FORMAT_BEG, FID_RUN },
	{ FIT_TOKEN, TID_RUN, TT_ALPHA },
	{ FIT_TOKEN, TID_ANY, TT_ANY }, /* at least one token for the command */
	{ FIT_PUNCT, FP_OPTL_BEG, TT_NULL },
	{ FIT_PUNCT, FP_REPT_BEG, TT_NULL },
	{ FIT_TOKEN, TID_ANY, TT_ANY }, /* any number of following arguments */
	{ FIT_PUNCT, FP_REPT_END, TT_NULL },
	{ FIT_PUNCT, FP_OPTL_END, TT_NULL },
	{ FIT_PUNCT, FP_FORMAT_END, FID_RUN },

	/* getprop key -- the value gets stuffed into the callback data */
	{ FIT_PUNCT, FP_FORMAT_BEG, FID_GETPROP },
	{ FIT_TOKEN, TID_GETPROP, TT_ALPHA },
	{ FIT_TOKEN, TID_ANY, TT_ANY }, /* property name */
	{ FIT_PUNCT, FP_FORMAT_END, FID_GETPROP },

	/* setprop key value */
	{ FIT_PUNCT, FP_FORMAT_BEG, FID_SETPROP },
	{ FIT_TOKEN, TID_SETPROP, TT_ALPHA },
	{ FIT_TOKEN, TID_ANY, TT_ANY }, /* property name */
	{ FIT_TOKEN, TID_ANY, TT_ANY }, /* value */
	{ FIT_PUNCT, FP_FORMAT_END, FID_SETPROP },

	/* killbypidfile key value */
	{ FIT_PUNCT, FP_FORMAT_BEG, FID_KILLBYPIDFILE },
	{ FIT_TOKEN, TID_KILLBYPIDFILE, TT_ALPHA },
	{ FIT_TOKEN, TID_ANY, TT_ANY }, /* signal (e.g., HUP, TERM) */
	{ FIT_TOKEN, TID_ANY, TT_ANY }, /* file */
	{ FIT_PUNCT, FP_FORMAT_END, FID_KILLBYPIDFILE },

	/* killbyname key value */
	{ FIT_PUNCT, FP_FORMAT_BEG, FID_KILLBYNAME },
	{ FIT_TOKEN, TID_KILLBYNAME, TT_ALPHA },
	{ FIT_TOKEN, TID_ANY, TT_ANY }, /* signal */
	{ FIT_TOKEN, TID_ANY, TT_ANY }, /* name */
	{ FIT_PUNCT, FP_FORMAT_END, FID_KILLBYNAME },

	{ FIT_FORMAT, FP_TABLE_END, FID_NULL }
};

/*
 * Private Implementation
 */
static char* tokensToString(TOKENINFO* pTokens, int nTokens)
{
	int size = 0;
	int i;

	for (i = 1; i < nTokens; ++i)
	{
		size += strlen(pTokens[i].pString);
	}

	/* allocate enough space for the tokens, spaces between and the zero */
	char* buffer = malloc(size + nTokens + 1);
	for (size = 0, i = 1; i < nTokens; ++i)
	{
		strcpy(buffer + size, pTokens[i].pString);
		size += strlen(pTokens[i].pString);
		buffer[size++] = ' ';
	}
	buffer[size] = '\0';

	return buffer;
}

int get_signal(char* signal_name)
{
	int signal = SIGTERM;

	if (!strcmp(signal_name, "USR1"))
	{
		signal = SIGUSR1;
	}
	else if (!strcmp(signal_name, "USR2"))
	{
		signal = SIGUSR2;
	}
	else if (!strcmp(signal_name, "HUP"))
	{
		signal = SIGHUP;
	}

	return signal;
}

int kill_processes_by_pidfile(FILE* logfp, const char* pidfile, int signal)
{
	FILE *pidFile = NULL;
	long pid = 0L;
	char buffer[READ_BUF_SIZE];

	if (!(pidFile = fopen(pidfile, "r")))
	{
		fprintf(logfp, "Failed to open pid file %s for reading -- the process is not (visibly) running.\n", pidfile);
		return -1;
	}
	if (fgets(buffer, READ_BUF_SIZE - 1, pidFile) == NULL)
	{
		fclose(pidFile);
		fprintf(logfp, "Can't get buffer from pid file: %s\n", pidfile);
		return -1;
	}
	fclose(pidFile);

	// Trying to kill
	pid = strtol(buffer, NULL, 0);
	fprintf(logfp, "Killing %ld with signal %d.\n", pid, signal);
	int result = kill(pid, signal);
	if (result != 0)
	{
		fprintf(logfp, "Unable to kill process (%s)\n", buffer);
		return -1;
	}
	return 0;
}

int kill_processes_by_name(FILE* logfp, const char* processName, int signal)
{
	int returncode = 0;

	DIR *dir = NULL;
	struct dirent *next;

	// open /proc
	dir = opendir("/proc");
	if (!dir)
		fprintf(logfp, "Can't open /proc \n");

	while ((next = readdir(dir)) != NULL)
	{
		FILE *status = NULL;
		char filename[READ_BUF_SIZE];
		char buffer[READ_BUF_SIZE];
		char name[READ_BUF_SIZE];

		/* Must skip ".." since that is outside /proc */
		if (strcmp(next->d_name, "..") == 0)
			continue;

		sprintf(filename, "/proc/%s/status", next->d_name);
		if (!(status = fopen(filename, "r")))
		{
			continue;
		}
		if (fgets(buffer, READ_BUF_SIZE - 1, status) == NULL)
		{
			fclose(status);
			continue;
		}
		fclose(status);

		/* Buffer should contain a string like "Name:   binary_name" */
		sscanf(buffer, "%*s %s", name);

		if ((strstr(name, processName)) != NULL)
		{
			// Trying to kill
			int result = kill(strtol(next->d_name, NULL, 0), signal);
			if (result != 0)
			{
				fprintf(logfp, "Unable to kill process %s (%s)\n", name,
						next->d_name);
				returncode = -1;
			}
		}
	}

	closedir(dir);

	return returncode;
}

static int property_get(const char *key, char *value, const char *default_value)
{
#ifdef ANDROID
	int len = __system_property_get(key, value);
#else
	int len = 0; /* just hack it for now */
#endif
	if (len > 0)
	{
		return len;
	}

	if (default_value)
	{
		len = strlen(default_value);
		memcpy(value, default_value, len + 1);
	}

	return len;
}

static void property_set(UpdaterInfo* pUI, const char *key, char *value)
{
	/* have to add space for the "su -c ..." -- the format gives a few bytes leeway */
	char* format = "su -c \"setprop %s %s\"";
	size_t len = strlen(key) + strlen(value) + strlen(format);
	char* buffer = malloc(len * sizeof(char));
	sprintf(buffer, format, key, value);
	pUI->callbackReturnData = buffer;
	fprintf(pUI->log_fp, "Set property by command: %s", buffer);
#ifdef ANDROID
	pUI->callbackReturnCode = system(pUI->callbackReturnData);
#else
	printf("\t ==> running: %s\n", buffer);
	pUI->callbackReturnCode = 1;
#endif
}

static void setCallbackData(UpdaterInfo* pUI, void* p)
{
	if (pUI->callbackReturnData != NULL)
	{
		free(pUI->callbackReturnData);
	}
	pUI->callbackReturnData = p;
}

static void RunFn(UpdaterInfo* pUI, TOKENINFO* pTokens, int nTokens,
		int nLineNo)
{
	int i, size;
	char* buffer = tokensToString(pTokens, nTokens);

	setCallbackData(pUI, buffer);
	fprintf(pUI->log_fp, "Running command: %s\n", buffer);
#ifdef ANDROID
	pUI->callbackReturnCode = system(pUI->callbackReturnData);
#else
	printf("\t ==> running: %s\n", buffer);
	pUI->callbackReturnCode = 1;
#endif
}

static void GetPropFn(UpdaterInfo* pUI, TOKENINFO* pTokens, int nTokens,
		int nLineNo)
{
	fprintf(pUI->log_fp, "Getting property: %s\n", pTokens[1].pString);
	char value[PROPERTY_VALUE_MAX];
	pUI->callbackReturnCode = property_get(pTokens[1].pString, value, "");
	setCallbackData(pUI, strdup(value));
	fprintf(pUI->log_fp, "Got property: %s, value: %s\n",
			pTokens[1].pString, value);
}

static void SetPropFn(UpdaterInfo* pUI, TOKENINFO* pTokens, int nTokens,
		int nLineNo)
{
	fprintf(pUI->log_fp, "Setting property: %s, value: %s",
			pTokens[1].pString, pTokens[2].pString);
	property_set(pUI, pTokens[1].pString, pTokens[2].pString);
	setCallbackData(pUI, strdup(""));
}

static void KillByPidFileFn(UpdaterInfo* pUI, TOKENINFO* pTokens, int nTokens,
		int nLineNo)
{
	int signal = get_signal(pTokens[1].pString);

	fprintf(pUI->log_fp, "Killing process by PID file %s with signal %s\n",
			pTokens[2].pString, pTokens[1].pString);
	kill_processes_by_pidfile(pUI->log_fp, pTokens[2].pString, signal);
	setCallbackData(pUI, strdup(""));
}

static void KillByNameFn(UpdaterInfo* pUI, TOKENINFO* pTokens, int nTokens,
		int nLineNo)
{
	int signal = get_signal(pTokens[1].pString);
	fprintf(pUI->log_fp, "Killing process by name %s with signal %s\n",
			pTokens[2].pString, pTokens[1].pString);
	kill_processes_by_name(pUI->log_fp, pTokens[2].pString, signal);
	setCallbackData(pUI, strdup(""));
}

static void UIPrintFn(UpdaterInfo* pUI, TOKENINFO* pTokens, int nTokens,
		int nLineNo)
{
	char* buffer;
	char* line;

	fprintf(pUI->cmd_pipe, "ui_print\n");

	buffer = tokensToString(pTokens, nTokens);

	line = strtok(buffer, "\n");
	while (line)
	{
		fprintf(pUI->cmd_pipe, "ui_print %s\n", line);
		line = strtok(NULL, "\n");
	}
	fprintf(pUI->cmd_pipe, "ui_print\n");

	pUI->callbackReturnCode = 0;
	setCallbackData(pUI, buffer);
}

static void UIPrintAction(UpdaterInfo* pUI, TOKENINFO* pTokens,
		int nTokens, int nLineNo)
{
	pUI->callbackReturnCode = 0;
	setCallbackData(pUI, pTokens[0].pString);
}

static int parseCallback(unsigned int nFormatId, TOKENINFO* pTokens,
		int nTokens, int nLineNo, void* pHook)
{
	boolean bReturn = TRUE;
	UpdaterInfo* pUI = (UpdaterInfo*) pHook;

	static int nCalls;

	switch (nFormatId)
	{
	case FID_COMMENT:
		/* do nothing at all */
		break;
	case FID_RUN:
		RunFn(pUI, pTokens, nTokens, nLineNo);
		break;
	case FID_GETPROP:
		GetPropFn(pUI, pTokens, nTokens, nLineNo);
		break;
	case FID_SETPROP:
		SetPropFn(pUI, pTokens, nTokens, nLineNo);
		break;
	case FID_UIPRINT_ACTION:
		UIPrintAction(pUI, pTokens, nTokens, nLineNo);
		break;
	case FID_KILLBYPIDFILE:
		KillByPidFileFn(pUI, pTokens, nTokens, nLineNo);
		break;
	case FID_KILLBYNAME:
		KillByNameFn(pUI, pTokens, nTokens, nLineNo);
		break;
	case FID_UIPRINT:
		UIPrintFn(pUI, pTokens, nTokens, nLineNo);
		break;
	default:
		fprintf(pUI->cmd_pipe,
				"ParseCallback called, but no match for \"%s\".\n",
				pTokens[0].pString);
		break;
	}

	/* Return TRUE means all went well */
	return bReturn;
}

/*
 * Public Implementation
 */
void initUpdaterInfo(UpdaterInfo* pUI, FILE* fpOut, FILE* fpLog)
{
	bzero(pUI, sizeof(UpdaterInfo));
	pUI->cmd_pipe = fpOut;
	pUI->log_fp = fpLog;
}

boolean
runScriptFile(char* pszScript, int fdCmdPipe, char* pszLogFile)
{
	int fd;
	boolean bReturn = bReturn = (fd = FileOpen(pszScript, OF_READ)) >= 0;

	if (bReturn)
	{
		/* setup file to send commands back to the parent process. */
		FILE* fpPipe = fdopen(fdCmdPipe, "wb");
		setlinebuf(fpPipe);

		/* setup file for logging. */
		FILE* fpLog = fopen(pszLogFile, "a");

		// Run the script
		UpdaterInfo ui;
		initUpdaterInfo(&ui, fpPipe, fpLog);

		bReturn = ParseFile(fd, NULL, TokenTable, FormatTable,
				parseCallback, FALSE, TRUE, TRUE, (void*) &ui);
	}

	return bReturn;
}

/*
 * TODO: Pass these from the Java side -- it's ridiculous to duplicate this info.
 */
#define SCRIPT_FILE "/data/data/com.tdhite.dnsqache/conf/scripter.scr"
#define LOG_FILE "/data/data/com.tdhite.dnsqache/var/scripter.log"

int
main(int argc, char** argv)
{
    int fd;
	boolean nReturn = FALSE;
	char* pszScript = SCRIPT_FILE;

	if (argc > 1)
	{
		pszScript = argv[1];
	}

	nReturn = runScriptFile(pszScript, STDOUT, LOG_FILE);

	return nReturn ? 0 : 1;
}
