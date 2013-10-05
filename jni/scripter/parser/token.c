/*
 * File:         token.c
 * Description:  Break up buffer into tokens and determine format type.
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

#define BUFFERSIZE  257

/* bFirst */
#define FIRST_DELIM TRUE
#define LAST_DELIM  FALSE

/* IsNumber bFirst */
#define IN_FIRST TRUE
#define IN_REST  FALSE

/*********************************** Macros *********************************/

#define IS_COMMENT(c) (bAllowComments && c == ';')
#define IS_EXTENDEDLINE(c) (bAllowExtendedLines && c == '\\')
#define IS_DELIM(c) (c == ' ' || c == '\t')
#define IS_DATADELIM(c,bFirst) (bFirst ? (c == '%') : (c == '%'))
#define IS_SECTIONDELIM(c,bFirst) (bFirst ? (c == '[') : (c == ']'))
#define IS_STRINGDELIM(c) (c == '"')
#define IS_SWITCH(c) (c == '/')
#define IS_ESCAPE_CHAR(c) (bAllowEscapeChars && c == '\\')

/////////////////////////////////////////////////////////////////////////////
// Static variables

boolean bAllowExtendedLines = FALSE;
boolean bAllowComments = FALSE;
boolean bAllowEscapeChars = FALSE;

static TOKENDEF * pTokenTable;
static char NullString[] = "";

static unsigned short s_nAlpha[8] = //  A-Z and a-z
		{ 0, //   0-15
				0, //  16-31
				0x0000, //  32-47
				0x0000, //  48-63
				0xFFFE, //  64-79  (65-79 set)
				0x07FF, //  80-95  (80-90 set)
				0xFFFE, //  96-111 (97-111 set)
				0x07FF // 112-127 (112-122 set)
		};

static unsigned short s_nPunct[8] = //  ! " # $ % & ' ( ) * + , - . /
		{ //  : ; < = > ? @ [ \ ] ^ _ ` { | } ~
		0, //   0-15
				0, //  16-31
				0xFFFE, //  32-47  (33-47 set)
				0xFC00, //  48-63  (58-63 set)
				0x0001, //  64-79  (64 set)
				0xF800, //  80-95  (91-95 set)
				0x0001, //  96-111 (96 set)
				0xC800 // 112-127 (123-126 set)
		};

/*
 * Public variables
 */

/*
 * Local Functions
 */

static boolean
GetTokenId(TOKENINFO *);

static boolean
IsAlpha(char);

static boolean
IsNumber(char, boolean, unsigned int *);

static boolean
IsPunct(char);

static char
TranslateEscape(char);

//----------------------------------------------------------------------------
// Function:    GetTokenId
// Description: Returns the token id or same char if not.
// Arguments:   pTokenList - pointer to the token list
//              nTokens    - number of tokens in the token list
// Returns:     nothing
//----------------------------------------------------------------------------
boolean GetTokenId(TOKENINFO* pToken)
{
	TOKENDEF * pTokenDef = pTokenTable;

	while (pTokenDef->wType)
	{
		if (pTokenDef->wType == pToken->wType)
		{
			if (pTokenDef->bCaseSensitive)
			{
				if (StrCmp(pToken->pString, pTokenDef->pString) == 0)
				{
					pToken->wID = pTokenDef->wID;
					break;
				}
				else
					pToken->wID = TID_NULL;
			}
			else
			{
				if (StrICmp(pToken->pString, pTokenDef->pString) == 0)
				{
					pToken->wID = pTokenDef->wID;
					break;
				}
				else
					pToken->wID = TID_NULL;
			}
		}
		pTokenDef++;
	}
	return (pToken->wID != ZERO);
}

/*
 * Function:    IsAlpha
 * Description: Determines whether or not the given character is an
 *              alpha character as defined by s_nAlpha.
 * Arguments:   c - the character to test
 * Returns:     TRUE if the character is an alpha character, FALSE if not.
 */
static boolean IsAlpha(char c)
{
	unsigned short nMask = 0x0001, nIndex = (unsigned short) (c) / 16, nRem =
			(unsigned short) (c) % 16;

	return (s_nAlpha[nIndex] & (nMask << nRem)) != 0;
}

/*
 * Function:    IsNumber
 * Description: Determines whether or not the given character could be
 *              part of a number.
 * Arguments:   TestChar - character to test
 *              bFirst   - TRUE if testing for first character in number
 *              pType    - location to receive the number type descriptor
 * Returns:
 */
static boolean IsNumber(char TestChar, boolean bFirst, unsigned int* pType)
{
	boolean bResult = TRUE;
	static boolean bInt;
	static boolean bHex;
	static boolean bReal;
	static boolean bDec;
	static boolean bExp;
	static boolean bExpSign;
	static boolean bHexChar;

	if (bFirst)
	{
		if ((TestChar >= '0') && (TestChar <= '9'))
		{
			bInt = bHex = bReal = TRUE;
			bExpSign = bDec = bExp = bHexChar = FALSE;
		}
		else if ((TestChar == '+') || (TestChar == '-'))
		{
			bInt = bReal = TRUE;
			bHex = FALSE;
			bExpSign = FALSE;
			bDec = bExp = bHexChar = FALSE;
		}
		else if (TestChar == '.')
		{
			bExpSign = bInt = bHex = FALSE;
			bReal = TRUE;
			bDec = bExp = bHexChar = FALSE;
		}
		else
			bResult = FALSE;
	}
	else
	{
		if ((TestChar >= '0') && (TestChar <= '9'))
		{
			/* do nothing, but preclude complains about suspicious semicolon */
			(void*) 0;
		}
		else if (((TestChar == '+') || (TestChar == '-')) && bReal && bExp
				&& !bExpSign)
		{
			bInt = bHex = FALSE;
			bExpSign = TRUE;
		}
		else if (((TestChar == 'E') || (TestChar == 'e')) && bReal && !bExp)
		{
			bInt = FALSE;
			bExp = TRUE;
		}
		else if (((TestChar >= 'A') && (TestChar <= 'F'))
				|| ((TestChar >= 'a') && (TestChar <= 'f')) && bHex && bHexChar)
			bInt = bReal = FALSE;
		else if ((TestChar == '.') && bReal && !bDec)
		{
			bInt = bHex = FALSE;
			bDec = TRUE;
		}
		else if (((TestChar == 'x') || (TestChar == 'X')) && bHex && !bHexChar)
		{
			bHexChar = TRUE;
			bInt = bReal = FALSE;
		}
		else if (TestChar == ',')
		{
			bHex = FALSE;
		}
		else
			bResult = FALSE;
	}
	*pType = !bResult ? *pType : bInt ? TT_INT : bReal ? TT_REAL : TT_HEX;
	return bResult;
}

/*
 * Function:    IsPunct
 * Description: Determines whether or not the given character is a
 *              punctuation character as defined by s_nPunct.
 * Arguments:   c - the character to test
 * Returns:     TRUE if the character is a punctuation character, FALSE if not.
 */
static boolean IsPunct(char c)
{
	unsigned short nMask = 0x0001;
	unsigned short nIndex = (unsigned short) (c) / 16;
	unsigned short nRem = (unsigned short) (c) % 16;

	return (s_nPunct[nIndex] & (nMask << nRem)) != 0;
}

/*
 * Function:    TranslateEscape
 * Description: Returns translated character if Escape is a valid escape
 *              or same char if not.
 * Arguments:   pTokenList - pointer to the token list
 *              nTokens    - number of tokens in the token list
 * Returns:     nothing
 */
static char TranslateEscape(char cEscape)
{
	switch (cEscape)
	{
	case 'r':
		cEscape = '\r';
		break;
	case 'n':
		cEscape = '\n';
		break;
	case 't':
		cEscape = '\t';
		break;
	}
	return (cEscape);
}

/*
 * Global Functions
 */

/*
 * Function:    FreeTokenList
 * Description: frees the given token list
 * Arguments:   pTokenList - pointer to the token list
 *              nTokens    - number of tokens in the token list
 * Returns:     nothing
 */
void FreeTokenList(TOKENINFO* pTokenList, int nTokens)
{
	for (; nTokens-- > 0; pTokenList++)
		if (pTokenList->pString && pTokenList->pString != NullString)
			MemFree(pTokenList->pString);
}

/*
 * Function:    FreeTokenTable
 * Description: frees the Token ID Description Table resource that was
 *              loaded with the LoadTokenTable function.
 * Arguments:   none
 * Returns:     nothing
 */
void FreeTokenTable()
{
	pTokenTable = NULL;
}

/*
 * Function:    GetToken
 * Description: used to break up a control file char buffer into tokens.
 * Arguments:   ppLine  - long pointer to pstring to be tokenized. After
 *                        the call it will point to the next token in the
 *                        string if a token was found else it points to the
 *                        NULL terminator.  pLine must be null terminated
 *                        char string.
 *              pToken  - long pointer to a token info struct to be filled
 *                        with the token TYPE, ID and STRING found.
 * Returns:     true if token found, false othersize
 */
boolean GetToken(char** ppLine, TOKENINFO* pToken)
{
	char * pNext = *ppLine;

	pToken->wType = TT_NULL;
	pToken->wID = TID_NULL;
	pToken->pString = NULL;

	while (*pNext && (pToken->wType == TID_NULL))
	{
		char StringBuf[BUFFERSIZE];
		boolean bSwitch = 0;
		int i = 0;

		if (IS_DELIM (*pNext))
			while (*(pNext = StrNext(pNext)) && IS_DELIM (*pNext))
				;

		if (IS_COMMENT (*pNext))
		{
			pToken->wType = TT_NULL;
			while (*(pNext = StrNext(pNext)))
				;
		}
		else if (IS_EXTENDEDLINE (*pNext))
		{
			pToken->wType = TT_EXTENDEDLINE;
			pNext = StrNext(pNext);
		}
		else if (IS_STRINGDELIM (*pNext))
		{
			pNext = StrNext(pNext);
			while (*pNext && !IS_STRINGDELIM (*pNext))
			{
				if (IS_ESCAPE_CHAR (*pNext))
				{
					pNext = StrNext(pNext);
					if (*pNext)
						*pNext = TranslateEscape(*pNext);
					else
						break;
				}
				StringBuf[i++] = *pNext;
				pNext = StrNext(pNext);
			}
			if (*pNext)
				pNext = StrNext(pNext);
			pToken->wType = TT_STRING;
		}
		else if (IS_SECTIONDELIM (*pNext,FIRST_DELIM))
		{
			pNext = StrNext(pNext);
			while (*pNext && !IS_SECTIONDELIM (*pNext,LAST_DELIM))
			{
				StringBuf[i++] = *pNext;
				pNext = StrNext(pNext);
			}
			if (*pNext)
				pNext = StrNext(pNext);
			pToken->wType = TT_SECTION;
		}
		else if (IS_DATADELIM (*pNext,FIRST_DELIM))
		{
			pNext = StrNext(pNext);
			while (*pNext && !IS_DATADELIM (*pNext,LAST_DELIM))
			{
				StringBuf[i++] = *pNext;
				pNext = StrNext(pNext);
			}
			if (*pNext)
				pNext = StrNext(pNext);
			pToken->wType = TT_DATA;
		}
		else if (/*(bSwitch = IS_SWITCH (*pNext)) ||*/IsAlpha(*pNext))
		{
			do
			{
				StringBuf[i++] = *pNext;
				pNext = StrNext(pNext);
			} while (*pNext && !IS_DELIM (*pNext)
					&& (bAllowExtendedLines || !IsPunct(*pNext)));
			pToken->wType = !bSwitch ? TT_ALPHA : i > 1 ? TT_SWITCH : TT_PUNCT;
		}
		else if (IsNumber(*pNext, IN_FIRST, &pToken->wType))
		{
			do
			{
				StringBuf[i++] = *pNext;
				pNext = StrNext(pNext);
			} while (*pNext && IsNumber(*pNext, IN_REST, &pToken->wType));

			if (pToken->wType == TT_NULL)
				while (*(pNext = StrNext(pNext)) && !IS_DELIM (*pNext))
					;
		}
		else if (IsPunct(*pNext) && !IS_DELIM (*pNext))
		{
			StringBuf[i++] = *pNext;
			pToken->wType = TT_PUNCT;
			pToken->wID = (unsigned int) *pNext;
			pNext = StrNext(pNext);
		}
		else if (*pNext)
			pNext = StrNext(pNext);
		if (i > 0)
		{
			if (pToken->pString)
				MemFree(pToken->pString);
			StringBuf[i] = '\0';
			if (pToken->pString = (char*) MemAlloc(i + 1))
				StrCpy(pToken->pString, StringBuf);
		}
	}
	if (pToken->pString == NULL)
		pToken->pString = NullString;
	if (pToken->wType)
	{
		if (pToken->wType == TT_INT && IsPunct(*pToken->pString)
				&& StrLen(pToken->pString) == 1)
			pToken->wType = TT_PUNCT;
		GetTokenId(pToken);
		if (IS_DELIM (*pNext))
			while (*(pNext = StrNext(pNext)) && IS_DELIM (*pNext))
				;
	}

	*ppLine = pNext;
	return (pToken->wType != ZERO);
}

/*
 * Function:    GetTokenList
 * Description: This function takes a char string passed in the pLine parameter
 *              and tokenizes it. The function breaks up the line into tokens
 *              using delimiters and puts them in the dynamic array
 *              pTokenBuffer.  The dymnamic array is composed
 *              of TOKENINFO data structures which contain the token ID,
 *              token type and the token string. The token ID's are
 *              determined from the Token ID description table in the resource
 *              file.  This table must be loaded before a call to this function
 *              is made.  This is done by calling LoadTokenTable (token table
 *              name).  The pointer to the BOOL extended line argument is
 *              used to ADD the existing token buffer without clearing it
 *              first. This is used to handle multiple line formats. It may be
 *              NULL if multiple line formats are of no concern.
 * Arguments:   pLine - pointer to the buffer containing the line
 *              pTokenList     - the token list to fill
 *              nTokens        - max number of tokens in pTokenList
 *              MaxTokens      - max number of tokens in pTokenList
 *              pbExtendedLine - where to store extended line flag when
 *                               such things are detected
 * Returns:     Number of tokens found in the line buffer
 */
int GetTokenList(char* pLine, TOKENINFO* pTokenList, int nTokens, int MaxTokens,
		boolean* pbExtendedLine)
{
	if (nTokens > 0 && ((pbExtendedLine == NULL) || !(*pbExtendedLine)))
	{
		FreeTokenList(pTokenList, nTokens);
		nTokens = 0;
	}
	if (pbExtendedLine)
		*pbExtendedLine = FALSE;
	while (nTokens < MaxTokens && GetToken(&pLine, pTokenList + nTokens))
	{
		if (pbExtendedLine && pTokenList[nTokens].wType == TT_EXTENDEDLINE)
		{
			*pbExtendedLine = TRUE;
			break;
		}
		nTokens++;
	}
	return nTokens;
}

/*
 * Function:    LoadTokenTable
 * Description: Loads the token table from the resource file.
 *              This is not active at this time.
 * Arguments:   pTokenDefs - pointer to token table to update
 *                           when the allocation occurs.
 * Returns:     TRUE if the token table was loaded, FALSE othersize
 */
boolean LoadTokenTable(TOKENDEF* pTokenDefs)
{
	return (pTokenTable = pTokenDefs) != NULL;
}

/*
 * Function:    ClearAlpha
 * Description: Clears the given character from the alpha character list.
 * Arguments:   c  -  character to clear
 * Returns:     nothing
 */
void ClearAlpha(char c)
{
	unsigned short nMask = 0x0001, nIndex = (unsigned short) (c) / 16, nRem =
			(unsigned short) (c) % 16;

	if (c >= 0 && c < 128)
		s_nAlpha[nIndex] &= ~(nMask << nRem);
}

/*
 * Function:    SetAlpha
 * Description: Set the given character in the alpha character list.
 * Arguments:   c  -  character to set
 * Returns:     nothing
 */
void SetAlpha(char c)
{
	unsigned short nMask = 0x0001, nIndex = (unsigned short) (c) / 16, nRem =
			(unsigned short) (c) % 16;

	if (c >= 0 && c < 128)
		s_nAlpha[nIndex] |= (nMask << nRem);
}

/*
 * Function:    ClearPunct
 * Description: Clears the given character from the punctuation list.
 * Arguments:   c  -  character to clear
 * Returns:     nothing
 */
void ClearPunct(char c)
{
	unsigned short nMask = 0x0001, nIndex = (unsigned short) (c) / 16, nRem =
			(unsigned short) (c) % 16;

	if (c >= 0 && c < 128)
		s_nPunct[nIndex] &= ~(nMask << nRem);
}

/*
 * Function:    SetPunct
 * Description: Set the given character in the punctuation list.
 * Arguments:   c  -  character to set
 * Returns:     nothing
 */
void SetPunct(char c)
{
	unsigned short nMask = 0x0001, nIndex = (unsigned short) (c) / 16, nRem =
			(unsigned short) (c) % 16;

	if (c >= 0 && c < 128)
		s_nPunct[nIndex] |= (nMask << nRem);
}
