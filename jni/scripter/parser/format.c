/*
 * File:         format.c
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

/* NextFormatItem - current types */
#define NFI_TOKEN     1
#define NFI_OR        2
#define NFI_OPTIONAL  3
#define NFI_REPEATS   4

#define TOKEN_MATCHES_FORMAT(Format,Token) \
	(((Format.ID == TID_ANY) ? TRUE : (Format.ID == Token.wID)) \
		&& ((Format.TokenType == TT_ANY) ? TRUE : \
			((Format.TokenType != TT_NUMBER) ? \
				(Format.TokenType == Token.wType) : \
				(Token.wType == TT_INT || Token.wType == TT_HEX || Token.wType == TT_REAL))))

#define IS_MORE_TOKENS(TokenIndex,nTokens) ((TokenIndex) < nTokens)

/*
 *Types
 */

/*
 * Static variables
 */

static FORMATDEF* m_pFormatTable;

/*
 * Public variables
 */

/*
 * Private Implementation
 */

static unsigned int
GetFormatIndex(unsigned int, FORMATDEF *);
static boolean
MoreRequiredFormatItems(FORMATDEF *, unsigned int);
static void
NextFormat(unsigned int *);
static void
NextFormatItem(unsigned int, FORMATDEF *, unsigned int *);
static boolean
TestFormat(TOKENINFO *, unsigned int *, unsigned int, unsigned int *);
static boolean
TestOr(TOKENINFO *, unsigned int *, unsigned int, unsigned int *);
static boolean
TestOptional(TOKENINFO *, unsigned int *, unsigned int, unsigned int *);
static boolean
TestRepeat(TOKENINFO *, unsigned int *, unsigned int, unsigned int *);

/*
 * Function:    GetFormatIndex
 * Description: This function returns the index in the format description
 *              table of the first item of the format specifed by the given ID.
 * Arguments:   wID    - the id to locate a format for
 *              pTable - the Format table which describes syntax formats
 * Returns:     the format id found, or 0 if none was found
 */
static unsigned int GetFormatIndex(unsigned int wID, FORMATDEF* pTable)
{
	unsigned int wIndex = 0;

	while (!((pTable[wIndex].FormatInfoType == FIT_PUNCT)
			&& (pTable[wIndex].ID == FP_FORMAT_BEG)
			&& (pTable[wIndex].TokenType == wID))
			&& (pTable[wIndex].ID != FP_TABLE_END))
		wIndex++;
	if (pTable[wIndex].ID != FP_TABLE_END)
		wIndex++;
	return wIndex;
}

/*
 * Function:    MoreRequiredFormatItems
 * Description: This function is called to see if there are any more required
 *              items for the current format. It searches from the current
 *              position in the format table for any non-optional items until
 *              it reaches the end of the format. If none were found it
 *              returns false indicating there are more required items.
 * Arguments:   pTable - the Format table which describes syntax formats
 *              Index  - the index into the format table to check
 * Returns:     the format id found, or 0 if none was found
 */
static boolean MoreRequiredFormatItems(FORMATDEF* pTable, unsigned int Index)
{
	unsigned int OptionalState = 0;
	boolean bRequiredTokens = FALSE;

	while (!bRequiredTokens)
	{
		if (pTable[Index].FormatInfoType == FIT_PUNCT)
		{
			if (pTable[Index].ID == FP_TABLE_END)
				break;
			else if (pTable[Index].ID == FP_FORMAT_END)
				break;
			else if (pTable[Index].ID == FP_OPTL_BEG)
			{
				OptionalState++;
			}
			else if (pTable[Index].ID == FP_OPTL_END && OptionalState > 0)
			{
				OptionalState--;
			}
			else if (OptionalState == 0&&
			pTable[Index].ID != FP_OPTL_END &&
			pTable[Index].ID != FP_REPT_END)bRequiredTokens = TRUE;
		}
		else if (pTable[Index].FormatInfoType == FIT_TOKEN ||
				pTable[Index].FormatInfoType == FIT_FORMAT)
		{
			bRequiredTokens = (OptionalState == 0);
		}
		else
		{
			bRequiredTokens = TRUE;
		}

		Index++;
	}
	return bRequiredTokens;
}

/*
 * Function:    NextFormat
 * Description: moves the FormatIndex the next format in
 *              the format id descripter table
 * Arguments:   pFormatIndex - pointer to current format
 * Returns:     nothing
 */
static void NextFormat(unsigned int* pFormatIndex)
{
	boolean bReachedTableEnd = FALSE;

	while (!(m_pFormatTable[*pFormatIndex].FormatInfoType == FIT_PUNCT
			&& m_pFormatTable[*pFormatIndex].ID == FP_FORMAT_END))
	{
		if (m_pFormatTable[*pFormatIndex].FormatInfoType == FIT_PUNCT
				&& m_pFormatTable[*pFormatIndex].ID == FP_TABLE_END)
		{
			bReachedTableEnd = TRUE;
			break;
		}
		(*pFormatIndex)++;
	}
	if (!bReachedTableEnd)
		(*pFormatIndex)++;
	return;
}

/*
 * Function:    NextFormatItem
 * Description: This functions moves the FormatIndex the next format item
 *              in the format id descripter table
 * Arguments:   wCurrentType - the current format type
 *              pTable       - pointer to the table to move in
 *              pIndex       - pointer to current format
 * Returns:     nothing
 */
static void NextFormatItem(unsigned int wCurrentType, FORMATDEF* pTable,
		unsigned int* pIndex)
{
	unsigned int nNestedOrs = 0;
	unsigned int nNestedOptionals = 0;
	unsigned int nNestedRepeats = 0;
	boolean bReachedNextItem = FALSE;

	while (!bReachedNextItem)
	{
		if (pTable[*pIndex].FormatInfoType == FIT_PUNCT)
		{
			if (wCurrentType == NFI_TOKEN)
				bReachedNextItem = TRUE;
			else if (pTable[*pIndex].ID == FP_OR_BEG)
				nNestedOrs++;
			else if (pTable[*pIndex].ID == FP_OPTL_BEG)
				nNestedOptionals++;
			else if (pTable[*pIndex].ID == FP_REPT_BEG)
				nNestedRepeats++;
			else if (pTable[*pIndex].ID == FP_OR_END)
			{
				nNestedOrs -= (nNestedOrs > 0 ? 1 : 0);
				if (nNestedOrs == ZERO && wCurrentType == NFI_OR)
					bReachedNextItem = TRUE;
			}
			else if (pTable[*pIndex].ID == FP_OPTL_END)
			{
				nNestedOptionals -= (nNestedOptionals > 0 ? 1 : 0);
				if (nNestedOptionals == ZERO && wCurrentType == NFI_OPTIONAL)
					bReachedNextItem = TRUE;
			}
			else if (pTable[*pIndex].ID == FP_REPT_END)
			{
				nNestedRepeats -= (nNestedRepeats > 0 ? 1 : 0);
				if (nNestedRepeats == ZERO && wCurrentType == NFI_REPEATS)
					bReachedNextItem = TRUE;
			}
			else if (pTable[*pIndex].ID == FP_FORMAT_END)
				bReachedNextItem = TRUE;
			else if (pTable[*pIndex].ID == FP_TABLE_END)
				break;

		}
		else if (pTable[*pIndex].FormatInfoType == FIT_TOKEN
				&& wCurrentType == NFI_TOKEN)
			bReachedNextItem = TRUE;
		(*pIndex)++;
	}
}

/*
 * Function:    TestFormat
 * Description: Determines if a token set matchs a format
 * Arguments:   pTokens      -
 *              pTokenIndex  -
 *              nTokens      -
 *              pFormatIndex -
 * Returns:     TRUE if a match was found, FALSE othersize
 */
static boolean TestFormat(TOKENINFO* pTokens, unsigned int* pTokenIndex,
		unsigned int nTokens, unsigned int* pFormatIndex)
{
	boolean bMatch = TRUE;
	boolean bError = FALSE;
	boolean bReachedFormatEnd = FALSE;
	boolean bReachedTokenEnd = !IS_MORE_TOKENS(*pTokenIndex,nTokens);
	unsigned int OriginalTokenIndex = *pTokenIndex;

	while (bMatch && !bReachedFormatEnd && !bReachedTokenEnd && !bError)
	{
		if (m_pFormatTable[*pFormatIndex].FormatInfoType == FIT_TOKEN)
		{
			if (TOKEN_MATCHES_FORMAT(m_pFormatTable[*pFormatIndex],
					pTokens[*pTokenIndex]))
			{
				bMatch = TRUE;
				(*pFormatIndex)++;
				(*pTokenIndex)++;
				bReachedTokenEnd = !IS_MORE_TOKENS(*pTokenIndex,nTokens);
			}
			else
				bMatch = FALSE;
		}
		else if (m_pFormatTable[*pFormatIndex].FormatInfoType == FIT_PUNCT)
		{
			switch (m_pFormatTable[*pFormatIndex].ID)
			{
			case FP_OPTL_BEG:
				(*pFormatIndex)++;
				if (TestOptional(pTokens, pTokenIndex, nTokens, pFormatIndex))
					bMatch = TRUE;
				break;
			case FP_OR_BEG:
				(*pFormatIndex)++;
				bMatch = TestOr(pTokens, pTokenIndex, nTokens, pFormatIndex);
				break;
			case FP_REPT_BEG:
				(*pFormatIndex)++;
				bMatch = TestRepeat(pTokens, pTokenIndex, nTokens,
						pFormatIndex);
				break;
			case FP_FORMAT_END:
				(*pFormatIndex)++;
				bReachedFormatEnd = TRUE;
				break;
			default:
				bError = TRUE;
				bMatch = FALSE;
				break;
			}
			bReachedTokenEnd = !IS_MORE_TOKENS(*pTokenIndex,nTokens);
		}
		else if (m_pFormatTable[*pFormatIndex].FormatInfoType == FIT_FORMAT)
		{
			unsigned int wTestFormatIndex = GetFormatIndex(
					m_pFormatTable[*pFormatIndex].ID, m_pFormatTable);
			bMatch = TestFormat(pTokens, pTokenIndex, nTokens,
					&wTestFormatIndex);
			if (bMatch)
				(*pFormatIndex)++;
			bReachedTokenEnd = !IS_MORE_TOKENS(*pTokenIndex,nTokens);
		}
	}
	if (bMatch && bReachedTokenEnd && !bReachedFormatEnd)
		bMatch = !MoreRequiredFormatItems(m_pFormatTable, *pFormatIndex);
	if (!bReachedFormatEnd)
		NextFormat(pFormatIndex);
	if (!bMatch)
		*pTokenIndex = OriginalTokenIndex;

	return bMatch;
}

/*
 * Function:    TestOr
 * Description: attempts a logical or of multiple formats against the
 *              token list to find a suitable format match
 * Arguments:   pTokens      -
 *              pTokenIndex  -
 *              nTokens      -
 *              pFormatIndex -
 * Returns:     TRUE if a match was found, FALSE othersize
 */
static boolean TestOr(TOKENINFO* pTokens, unsigned int* pTokenIndex,
		unsigned int nTokens, unsigned int* pFormatIndex)
{
	boolean bMatch = FALSE;
	boolean bError = FALSE;
	boolean bReachedOrEnd = FALSE;
	boolean bReachedTokenEnd = !IS_MORE_TOKENS(*pTokenIndex,nTokens);

	while (!bMatch && !bReachedOrEnd && !bReachedTokenEnd && !bError)
	{
		if (m_pFormatTable[*pFormatIndex].FormatInfoType == FIT_TOKEN)
		{
			if (TOKEN_MATCHES_FORMAT(m_pFormatTable[*pFormatIndex],
					pTokens[*pTokenIndex]))
			{
				bMatch = TRUE;
				(*pFormatIndex)++;
				(*pTokenIndex)++;
				bReachedTokenEnd = !IS_MORE_TOKENS(*pTokenIndex, nTokens);
			}
			else
				(*pFormatIndex)++;
		}
		else if (m_pFormatTable[*pFormatIndex].FormatInfoType == FIT_PUNCT)
		{
			switch (m_pFormatTable[*pFormatIndex].ID)
			{
			case FP_OPTL_BEG:
				(*pFormatIndex)++;
				if (TestOptional(pTokens, pTokenIndex, nTokens, pFormatIndex))
					bMatch = TRUE;
				break;
			case FP_OR_BEG:
				(*pFormatIndex)++;
				bMatch = TestOr(pTokens, pTokenIndex, nTokens, pFormatIndex);
				break;
			case FP_REPT_BEG:
				(*pFormatIndex)++;
				bMatch = TestRepeat(pTokens, pTokenIndex, nTokens,
						pFormatIndex);
				break;
			case FP_OR_END:
				(*pFormatIndex)++;
				bReachedOrEnd = TRUE;
				break;
			default:
				bError = TRUE;
				bMatch = FALSE;
				break;
			}
			bReachedTokenEnd = !IS_MORE_TOKENS(*pTokenIndex,nTokens);
		}
		else if (m_pFormatTable[*pFormatIndex].FormatInfoType == FIT_FORMAT)
		{
			unsigned int wTestFormatIndex = GetFormatIndex(
					m_pFormatTable[*pFormatIndex].ID, m_pFormatTable);
			bMatch = TestFormat(pTokens, pTokenIndex, nTokens,
					&wTestFormatIndex);
			(*pFormatIndex)++;
			bReachedTokenEnd = !IS_MORE_TOKENS(*pTokenIndex,nTokens);
		}
	}
	if (!bReachedOrEnd)
		NextFormatItem(NFI_OR, m_pFormatTable, pFormatIndex);

	return bMatch;
}

/*
 * Function:    TestOptional
 * Description: tests optional formats against the
 *              token list to find a suitable format match
 * Arguments:   pTokens      -
 *              pTokenIndex  -
 *              nTokens      -
 *              pFormatIndex -
 * Returns:     TRUE if a match was found, FALSE othersize
 */
static boolean TestOptional(TOKENINFO* pTokens, unsigned int* pTokenIndex,
		unsigned int nTokens, unsigned int* pFormatIndex)
{
	boolean bMatch = TRUE;
	boolean bError = FALSE;
	boolean bReachedOptionalEnd = FALSE;
	boolean bReachedTokenEnd = !IS_MORE_TOKENS(*pTokenIndex,nTokens);
	unsigned int OriginalTokenIndex = *pTokenIndex;

	while (bMatch && !bReachedOptionalEnd && !bReachedTokenEnd && !bError)
	{
		if (m_pFormatTable[*pFormatIndex].FormatInfoType == FIT_TOKEN)
		{
			if (TOKEN_MATCHES_FORMAT(m_pFormatTable[*pFormatIndex],
					pTokens[*pTokenIndex]))
			{
				bMatch = TRUE;
				(*pFormatIndex)++;
				(*pTokenIndex)++;
				bReachedTokenEnd = !IS_MORE_TOKENS(*pTokenIndex,nTokens);
			}
			else
				bMatch = FALSE;
		}
		else if (m_pFormatTable[*pFormatIndex].FormatInfoType == FIT_PUNCT)
		{
			switch (m_pFormatTable[*pFormatIndex].ID)
			{
			case FP_OPTL_BEG:
				(*pFormatIndex)++;
				if (TestOptional(pTokens, pTokenIndex, nTokens, pFormatIndex))
					bMatch = TRUE;
				break;
			case FP_OR_BEG:
				(*pFormatIndex)++;
				bMatch = TestOr(pTokens, pTokenIndex, nTokens, pFormatIndex);
				break;
			case FP_REPT_BEG:
				(*pFormatIndex)++;
				bMatch = TestRepeat(pTokens, pTokenIndex, nTokens,
						pFormatIndex);
				break;
			case FP_OPTL_END:
				(*pFormatIndex)++;
				bReachedOptionalEnd = TRUE;
				break;
			default:
				bError = TRUE;
				bMatch = FALSE;
				break;
			}
			bReachedTokenEnd = !IS_MORE_TOKENS(*pTokenIndex,nTokens);
		}
		else if (m_pFormatTable[*pFormatIndex].FormatInfoType == FIT_FORMAT)
		{
			unsigned int wTestFormatIndex = GetFormatIndex(
					m_pFormatTable[*pFormatIndex].ID, m_pFormatTable);
			bMatch = TestFormat(pTokens, pTokenIndex, nTokens,
					&wTestFormatIndex);
			if (bMatch)
				(*pFormatIndex)++;
			bReachedTokenEnd = !IS_MORE_TOKENS(*pTokenIndex,nTokens);
		}
	}
	if (bMatch && bReachedTokenEnd && !bReachedOptionalEnd)
		bMatch = !MoreRequiredFormatItems(m_pFormatTable, *pFormatIndex);
	if (!bReachedOptionalEnd)
		NextFormatItem(NFI_OPTIONAL, m_pFormatTable, pFormatIndex);
	if (!bMatch)
		*pTokenIndex = OriginalTokenIndex;

	return bMatch;
}

/*
 * Function:    TestRepeat
 * Description: tests optional formats against a repeating set of
 *              formats to find a suitable format match
 * Arguments:   pTokens      -
 *              pTokenIndex  -
 *              nTokens      -
 *              pFormatIndex -
 * Returns:     TRUE if a match was found, FALSE othersize
 */
static boolean TestRepeat(TOKENINFO* pTokens, unsigned int* pTokenIndex,
		unsigned int nTokens, unsigned int* pFormatIndex)
{
	boolean bMatch = TRUE;
	boolean bError = FALSE;
	boolean bReachedTokenEnd = !IS_MORE_TOKENS(*pTokenIndex,nTokens);
	unsigned int StartFormatIndex = *pFormatIndex;
	unsigned int StartTokenIndex = *pTokenIndex;
	int MatchCount = 0;

	while (bMatch && !bReachedTokenEnd && !bError)
	{
		if (m_pFormatTable[*pFormatIndex].FormatInfoType == FIT_TOKEN)
		{
			if (TOKEN_MATCHES_FORMAT(m_pFormatTable[*pFormatIndex],
					pTokens[*pTokenIndex]))
			{
				bMatch = TRUE;
				(*pFormatIndex)++;
				(*pTokenIndex)++;
				bReachedTokenEnd = !IS_MORE_TOKENS(*pTokenIndex,nTokens);
			}
			else
				bMatch = FALSE;
		}
		else if (m_pFormatTable[*pFormatIndex].FormatInfoType == FIT_PUNCT)
		{
			switch (m_pFormatTable[*pFormatIndex].ID)
			{
			case FP_OPTL_BEG:
				(*pFormatIndex)++;
				if (TestOptional(pTokens, pTokenIndex, nTokens, pFormatIndex))
					bMatch = TRUE;
				break;
			case FP_OR_BEG:
				(*pFormatIndex)++;
				bMatch = TestOr(pTokens, pTokenIndex, nTokens, pFormatIndex);
				break;
			case FP_REPT_BEG:
				(*pFormatIndex)++;
				bMatch = TestRepeat(pTokens, pTokenIndex, nTokens,
						pFormatIndex);
				break;
			case FP_REPT_END:
				*pFormatIndex = StartFormatIndex;
				StartTokenIndex = *pTokenIndex;
				MatchCount++;
				break;
			default:
				bError = TRUE;
				bMatch = FALSE;
				break;
			}
			bReachedTokenEnd = !IS_MORE_TOKENS(*pTokenIndex,nTokens);
		}
		else if (m_pFormatTable[*pFormatIndex].FormatInfoType == FIT_FORMAT)
		{
			unsigned int wTestFormatIndex = GetFormatIndex(
					m_pFormatTable[*pFormatIndex].ID, m_pFormatTable);
			bMatch = TestFormat(pTokens, pTokenIndex, nTokens,
					&wTestFormatIndex);
			if (bMatch)
				(*pFormatIndex)++;
			bReachedTokenEnd = !IS_MORE_TOKENS(*pTokenIndex,nTokens);
		}
	}
	if (!bError)
	{
		if (!bMatch)
		{
			bMatch = MatchCount > 0;
			*pTokenIndex = StartTokenIndex;
		}
		else if (bReachedTokenEnd)
			bMatch = !MoreRequiredFormatItems(m_pFormatTable, *pFormatIndex);
		NextFormatItem(NFI_REPEATS, m_pFormatTable, pFormatIndex);
	}
	return bMatch;
}

/*
 * Protected Implementation
 */

/*
 *Public Implementation
 */

/*
 * Function:    FreeFormatTable
 * Description: frees the FORMAT ID Description Table resource that was
 *              loaded with the LoadFormatTable function.
 * Arguments:   none
 * Returns:     nothing
 */
void FreeFormatTable()
{
	m_pFormatTable = NULL;
}

/*
 * Function:    GetFormatId
 * Description: This function compares the tokens in the token buffer against
 *              a format table found in the resource file. It determines what
 *              format if any matches the tokens in the buffer and returns the
 *              corresponding format id. The TokenBuffer is the argument
 *              returned from the get_ctl_tokens function. This buffer contains
 *              an array of TOKENINFO structure which conatins the token
 *              string the token ID and the token type. The Format table
 *              found in the resource is a tabularized BNF format
 *              table that describes the format syntax.
 * Arguments:   pTokenList - the token list to use to locate a valid format for
 *              nTokens    - number of tokens in pTokenList
 * Returns:     format id found or 0 if not found
 */
unsigned int GetFormatId(TOKENINFO* pTokenList, int nTokens)
{
	unsigned int TokenIndex = 0;
	unsigned int FormatTableIndex = 0;
	unsigned int wFormatID = 0;
	boolean bFoundMatch = FALSE;

	if (m_pFormatTable && pTokenList)
	{
		while (m_pFormatTable[FormatTableIndex].FormatInfoType == FIT_PUNCT
				&& m_pFormatTable[FormatTableIndex].ID == FP_FORMAT_BEG)
		{
			wFormatID = m_pFormatTable[FormatTableIndex].TokenType;
			FormatTableIndex++;
			TokenIndex = 0;
			bFoundMatch = TestFormat(pTokenList, &TokenIndex, nTokens,
					&FormatTableIndex) &&
					!IS_MORE_TOKENS(TokenIndex,(unsigned int)nTokens);if
(			bFoundMatch)
			break;
		}
	}
	return (bFoundMatch ? wFormatID : ZERO);
}

/*
 * Function:    LoadFormatTable
 * Description: loads the FORMAT ID Description Table resource
 * Arguments:   pFormatDefs - pointer to the format table to fill
 * Returns:     TRUE if the resource was loaded
 */
boolean LoadFormatTable(FORMATDEF* pFormatDefs)
{
	return (m_pFormatTable = pFormatDefs) != NULL;
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
