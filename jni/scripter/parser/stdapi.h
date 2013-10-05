/*
 * File:         stdapi.h
 * Description:  main header file for the parser library
 * Author:       Bill McGrane/Tom Hite
 */

#ifndef _STDAPI_H_
#define _STDAPI_H_

#include <stdlib.h>
#include <string.h>

/*
 * Macros
 */

/* DOS/Windows apps need 2 chars to end a line, so beware! */
#ifndef ENDOFLINE_CHAR_COUNT
	#define ENDOFLINE_CHAR_COUNT 1
#endif

#ifndef ZER0
	#define ZERO                0
#endif

#ifndef TRUE
#define TRUE 1
#endif
#ifndef FALSE
#define FALSE ZERO
#endif

#define STDIN           0
#define STDOUT          1
#define STDERR          2

#ifndef OF_READ
#define OF_READ         0x0000
#endif
#ifndef OF_WRITE
#define OF_WRITE        0x0001
#endif
#ifndef OF_READWRITE
#define OF_READWRITE    0x0002
#endif
#ifndef OF_TEXT
#define OF_TEXT         0x4000
#endif
#ifndef OF_BINARY
#define OF_BINARY       0x8000
#endif

#ifndef CF_READ
#define CF_READ         0000400
#endif
#ifndef CF_WRITE
#define CF_WRITE        0000200
#endif

#ifndef SF_START
#define SF_START        0
#endif
#ifndef SF_CURRENT
#define SF_CURRENT      1
#endif
#ifndef SF_END
#define SF_END          2
#endif

#define FileValid(fd)                   ((int)(fd) >= 0)
#define FileOpen(pszFile,Mode)          open(pszFile,Mode,0)
#define FileClose(fd)                   close(fd)
#define FileCreate(pszFile,Mode)        creat(pszFile,Mode)
#define FileSeek(fd,Ofs,Mode)           lseek(fd,Ofs,Mode)
#define FileTell(fd)                    tell(fd)
#define FileRead(fd,pData,Size)         read(fd,pData,Size)
#define FileWrite(fd,pData,Size)        write(fd,pData,Size)
#define FileDup(fd)                     dup(fd)
#define FileDup2(fdSrc,fdTgt)           dup2(fdSrc,fdTgt)
#define FileDelete(pszFilename)         remove(pszFilename)
#define FileTemp(pszFilename)           tmpnam(pszFilename)

#define MemCopy(pDst,pSrc,Size)         memcpy(pDst,pSrc,Size)
#define MemClear(pData,Size)            memset(pData,0,Size)
#define MemAlloc(Size)                  malloc(Size)
#define MemExpand(pData,Size)           realloc(pData,Size)
#define MemFree(pData)                  free(pData)
#define StrLen(pStr)                    strlen(pStr)
#define StrCpy(pStr1,pStr2)             strcpy(pStr1,pStr2)
#define StrCat(pStr1,pStr2)             strcat(pStr1,pStr2)
#define StrCmp(pStr1,pStr2)             strcmp(pStr1,pStr2)
#define StrICmp(pStr1,pStr2)            strcmp(pStr1,pStr2)
#define StrNCpy(pStr1,pStr2,Size)       strncpy(pStr1,pStr2,Size)
#define StrChr(pStr,Ch)                 strchr(pStr,Ch)
#define LongToStr(ulNum,pStr,Base)      ultoa(ulNum,pStr,Base)
#define IntToStr(iNum,pStr,Base)        itoa(iNum,pStr,Base)
#define StrToLong(pStr,ppszEnd,Base)    strtol(pStr,ppEnd,Base)
#define StrToInt(pStr)                  atoi(pStr)
#define StrNext(pStr)                   ((pStr) + 1)

/*
 * Types
 */
typedef int boolean;

#endif /* _STDAPI_H_ */
