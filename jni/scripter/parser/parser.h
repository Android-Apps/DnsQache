/*
 * File:         parser.h
 * Description:  main header file for the parser library
 * Author:       Bill McGrane/Tom Hite
 */

#ifndef _PARSER_H_
#define _PARSER_H_

#ifdef __cplusplus
extern "C"
{
#endif

/*
 * Macros
 */

#ifndef ZER0
	#define ZERO                0
#endif

/* parser section */
#define TT_NULL             0           /* token types */
#define TT_COMMENT          1
#define TT_ALPHA            2
#define TT_STRING           3
#define TT_SECTION          4
#define TT_DATA             5
#define TT_NUMBER           6
#define TT_INT              7
#define TT_REAL             8
#define TT_HEX              9
#define TT_PUNCT            10
#define TT_SWITCH           11
#define TT_EXTENDEDLINE     100
#define TT_ANY              0xFFFF

#define TID_NULL            0           /* reserved token IDs */
#define TID_ANY             0xFFFF

#define FIT_NULL            0           /* format info types */
#define FIT_PUNCT           1
#define FIT_TOKEN           2
#define FIT_FORMAT          3

#define FP_FORMAT_BEG       1           /* format punctuation */
#define FP_OPTL_BEG         2
#define FP_REPT_BEG         3
#define FP_OR_BEG           4
#define FP_OR_END           5
#define FP_REPT_END         6
#define FP_OPTL_END         7
#define FP_FORMAT_END       8
#define FP_TABLE_END        9

#define FID_NULL            0           /* reserved format IDs */

#define SIS_IGNORECASE      FALSE
#define SIS_CASESENSITIVE   TRUE

/*
 * Types
 */

typedef struct
{
	unsigned int FormatInfoType;
	unsigned int ID;
	unsigned int TokenType;
} FORMATDEF;

typedef struct
{
	unsigned int wType;
	unsigned int wID;
	char* pString;
} TOKENINFO;

typedef struct
{
	unsigned int wID;
	unsigned int wType;
	boolean bCaseSensitive;
	char* pString;
} TOKENDEF;

typedef boolean
(*PARSEPROC)(unsigned int, TOKENINFO*, int, int, void* pHook);

struct _tagParser
{
	/*
	 private:
	 */
	boolean m_bAllowExtendedLines;
	boolean m_bAllowComments;
	TOKENDEF* m_pTokenTable;
	char m_szDelimiters[20] /*  = " ," */;
	char m_NullString[2] /* = "" */;
	FORMATDEF* m_pFormatTable;

	/*
	 private:
	 unsigned int GetFormatIndex(unsigned int wID, FORMATDEF* pTable);
	 BOOL         MoreRequiredFormatItems(FORMATDEF* pTable, unsigned int Index);
	 void         NextFormat(unsigned int* pFormatIndex);
	 void         NextFormatItem(unsigned int  wCurrentType,
	 FORMATDEF*    pTable,
	 unsigned int* pIndex);
	 BOOL         TestFormat(TOKENINFO*    pTokens,
	 unsigned int* pTokenIndex,
	 unsigned int  nTokens,
	 unsigned int* pFormatIndex);
	 BOOL         TestOr(TOKENINFO*    pTokens,
	 unsigned int* pTokenIndex,
	 unsigned int  nTokens,
	 unsigned int* pFormatIndex);
	 BOOL         TestOptional(TOKENINFO*    pTokens,
	 unsigned int* pTokenIndex,
	 unsigned int  nTokens,
	 unsigned int* pFormatIndex);
	 BOOL         TestRepeat(TOKENINFO*    pTokens,
	 unsigned int* pTokenIndex,
	 unsigned int  nTokens,
	 unsigned int* pFormatIndex);
	 BOOL         IsNumber(char TestChar, BOOL bFirst, unsigned int* pType);
	 BOOL         GetTokenId(TOKENINFO* pToken);
	 char         TranslateEscape(char cEscape);

	 protected:
	 void         FreeFormatTable();
	 unsigned int GetFormatId(TOKENINFO* pTokenList, int nTokens);
	 BOOL         LoadFormatTable(FORMATDEF* pFormatDefs);
	 void         FreeTokenList(TOKENINFO* pTokenList, int nTokens);
	 void         FreeTokenTable()
	 BOOL         GetToken(char** ppLine, TOKENINFO* pToken);
	 int          GetTokenList(char*       pLine,
	 TOKENINFO*  pTokenList,
	 int         nTokens,
	 int         MaxTokens,
	 boolean*    pbExtendedLine);
	 boolean     LoadTokenTable(TOKENDEF*);

	 public:
	 boolean ParseFile(
	 int        fd,
	 char*      pSection,
	 TOKENDEF*  pTokenTable,
	 FORMATDEF* pFormatTable,
	 PARSEPROC  pParseProc,
	 boolean    bExtendLines,
	 boolean    bComments,
	 boolean    bEscapeChars,
	 void*      pHook);
	 */
};

/*
 * Public Variables
 */

/*
 * Public Functions
 */

/* from format.cpp */

void
FreeFormatTable(void);

unsigned int
GetFormatId(TOKENINFO*, int);

boolean
LoadFormatTable(FORMATDEF*);

/* from token.c */

extern boolean bAllowExtendedLines;
extern boolean bAllowComments;
extern boolean bAllowEscapeChars;

void
ClearAlpha(char);

void
SetAlpha(char);

void
ClearPunct(char);

void
SetPunct(char);

void
FreeTokenList(TOKENINFO*, int);

void
FreeTokenTable(void);

boolean
GetToken(char**, TOKENINFO*);

boolean
GetTokenList(char*, TOKENINFO*, int, int, boolean*);

boolean
LoadTokenTable(TOKENDEF*);

boolean
ParseFile(int fd, char* pSection, TOKENDEF* pTokenTable,
		FORMATDEF* pFormatTable, PARSEPROC pParseProc, boolean bExtendLines,
		boolean bComments, boolean bEscapeChars, void* pHook);

boolean
ParseBuffer(char* pBuffer, char* pSection, TOKENDEF* pTokenTable,
		FORMATDEF* pFormatTable, PARSEPROC pParseProc, boolean bExtendLines,
		boolean bComments, boolean bEscapeChars, void* pHook);

/*
 * Inlines
 */

#ifdef __cplusplus
}
#endif

#endif
