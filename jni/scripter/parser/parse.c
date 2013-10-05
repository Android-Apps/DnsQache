/*
 * File:         parse.c
 * Description:  Determine format types and process tokenized data.
 * Author:       Bill McGrane/Tom Hite
 */

#include <stdapi.h>
#include <parser.h>

/*
 * Debugging Issues
 */

/*
 * Macros/Maps
 */

#define MAXTOKENS       256
#define LINEBUFFERSIZE  257

#define endOfLine(c) (c == '\n' || c == '\r')

/*
 * Static variables
 */

/*
 * Public variables
 */

/*
 * Private Implementation
 */

/*
 * This function reads a line from the file into pBuffer and sets the file
 * pointer to the next line. It returns TRUE if there is more data or
 * if there are no errors.
 */
static boolean ReadLine(int fd, char* pBuffer, unsigned int wBufferMax)
{
	int i;
	int nBytesRead = FileRead(fd, pBuffer, wBufferMax - 1);
	boolean bResult = nBytesRead > 0;

	if (bResult)
	{
		for (i = 0; i < nBytesRead; i++)
		{
			if (endOfLine(pBuffer[i]))
				break;
		}
		pBuffer[i] = 0;
		if (i < nBytesRead)
			FileSeek(fd, (unsigned long) i - nBytesRead + ENDOFLINE_CHAR_COUNT,
				SF_CURRENT);
	}
	else
		bResult = FALSE;

	return bResult;
}

/*
 * This function reads a line from the pInput into pBuffer and sets the cursor
 * pointer to the next line. It returns TRUE if there is more data or
 * if there are no errors.
 */
static boolean ReadBufferLine(char const* pInput, int* pnPos, char* pBuffer,
		unsigned int wBufferMax)
{
	int i;
	boolean bResult = (boolean) (pInput[*pnPos]);

	if (bResult)
	{
		for (i = 0; pBuffer[i] = pInput[*pnPos]; (*pnPos)++, i++)
		{
			if (endOfLine(pBuffer[i]))
				break;
		}
		pBuffer[i] = 0;
	}

	return bResult;
}

/*
 * Protected Implementation
 */

/*
 * Public Implementation
 */

/*
 * Function:    ParseFile
 * Description: Parses the file and calls the callback function when necessary
 * Arguments:   hFile          - the file containing the data to be parsed
 *              pSection       - the section of the file, enclosed in [],
 *                               to parse
 *              pTokenTable    - the token table to use for token recognition
 *              pFormatTable   - table of 'syntax' formats to recognize
 *              pParseProc     - the callback function to call with each
 *                               complete statements found
 *              bExtendedLines - whether to allow extended lines with a \ char
 *              bComments      - whether to consider # lines as comment
 *              bEscapeChars   - whether to allow escape characters (e.g. '\n')
 *              dwHook         - 32 bit value passed to the callback function
 *                               which is user definable
 * Returns:     length in characters of the string
 */
boolean ParseFile(int fd, char* pSection, TOKENDEF* pTokenTable,
		FORMATDEF* pFormatTable, PARSEPROC pParseProc, boolean bExtendLines,
		boolean bComments, boolean bEscapeChars, void* pHook)
{
	char szLineBuffer[LINEBUFFERSIZE];
	boolean bReadingSection = FALSE;
	boolean bOk = LoadTokenTable(pTokenTable) && LoadFormatTable(pFormatTable);

	bAllowExtendedLines = bExtendLines;
	bAllowComments = bComments;
	bAllowEscapeChars = bEscapeChars;

	if (bOk)
	{
		TOKENINFO TokenList[MAXTOKENS];
		int nTokens = 0;
		int nLines = 0;

		while (ReadLine(fd, szLineBuffer, LINEBUFFERSIZE))
		{
			boolean bExtendedLine;
			char * pLineBuffer = (char *) szLineBuffer;

			nLines++;
			if (pSection && GetToken(&pLineBuffer, TokenList))
			{
				if (TokenList[0].wType == TT_SECTION)
				{
					if (bReadingSection)
						break;
					else if (StrICmp(pSection, TokenList[0].pString) == ZERO)
						bReadingSection = TRUE;
				}
				FreeTokenList(TokenList, 1);
			}
			if ((!pSection || bReadingSection)
					&& (nTokens = GetTokenList(szLineBuffer, TokenList, nTokens,
							MAXTOKENS, &bExtendedLine)) > 0 && !bExtendedLine
					&& !(bOk = (*pParseProc)(GetFormatId(TokenList, nTokens),
							TokenList, nTokens, nLines, pHook)))
			{	
				break;
			}
		}
		FreeTokenList(TokenList, nTokens);
	}
	FreeTokenTable();
	FreeFormatTable();
	return bOk;
}

/*
 * Function:    ParseBuffer
 * Description: Parses the buffer and calls the callback function when necessary
 * Arguments:   pBuffer        - the buffer containing the data to be parsed
 *              pSection       - the section of the buffer, enclosed in [],
 *                               to parse
 *              pTokenTable    - the token table to use for token recognition
 *              pFormatTable   - table of 'syntax' formats to recognize
 *              pParseProc     - the callback function to call with each
 *                               complete statements found
 *              bExtendedLines - whether to allow extended lines with a \ char
 *              bComments      - whether to consider # lines as comment
 *              bEscapeChars   - whether to allow escape characters (e.g. '\n')
 *              dwHook         - 32 bit value passed to the callback function
 *                               which is user definable
 * Returns:     length in characters of the string
 */
boolean ParseBuffer(char* pBuffer, char* pSection, TOKENDEF* pTokenTable,
		FORMATDEF* pFormatTable, PARSEPROC pParseProc, boolean bExtendLines,
		boolean bComments, boolean bEscapeChars, void* pHook)
{
	char szLineBuffer[LINEBUFFERSIZE];
	boolean bReadingSection = FALSE;
	boolean bOk = LoadTokenTable(pTokenTable) && LoadFormatTable(pFormatTable);
	int nCursor = 0;

	bAllowExtendedLines = bExtendLines;
	bAllowComments = bComments;
	bAllowEscapeChars = bEscapeChars;

	if (bOk)
	{
		TOKENINFO TokenList[MAXTOKENS];
		int nTokens = 0;
		int nLines = 0;

		while (ReadBufferLine(pBuffer, &nCursor, szLineBuffer, LINEBUFFERSIZE))
		{
			boolean bExtendedLine;
			char * pLineBuffer = (char *) szLineBuffer;

			nLines++;
			if (pSection && GetToken(&pLineBuffer, TokenList))
			{
				if (TokenList[0].wType == TT_SECTION)
					if (bReadingSection)
						break;
					else if (StrICmp(pSection, TokenList[0].pString) == ZERO)
						bReadingSection = TRUE;
				FreeTokenList(TokenList, 1);
			}
			if ((!pSection || bReadingSection)
					&& (nTokens = GetTokenList(szLineBuffer, TokenList, nTokens,
							MAXTOKENS, &bExtendedLine)) > 0 && !bExtendedLine
					&& !(bOk = (*pParseProc)(GetFormatId(TokenList, nTokens),
							TokenList, nTokens, nLines, pHook)))
				break;
		}
		FreeTokenList(TokenList, nTokens);
	}
	FreeTokenTable();
	FreeFormatTable();
	return bOk;
}

/*
 * Diagnostics
 */

/*
 * Construction/Destruction
 */

/*
 * Command/Message handlers
 */

