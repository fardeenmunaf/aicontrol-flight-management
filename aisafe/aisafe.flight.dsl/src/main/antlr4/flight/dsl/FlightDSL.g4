grammar FlightDSL;

// PARSER RULES

start
    : flightPlan+ EOF
    ;

flightPlan
    : FLIGHT ID LBRACE
        TYPE COLON flightType SEMI
        AIRCRAFT COLON ID SEMI
        ROUTE COLON ID FROM airportCode TO airportCode SEMI
        leg+
      RBRACE
    ;

flightType
    : REGULAR
    | CHARTER
    ;

leg
    : LEG LBRACE
        departure
        arrival
        fuel
        profile?
        segment+
      RBRACE
    ;

departure
    : DEPARTURE LBRACE
        AIRPORT COLON airportCode SEMI
        DATE COLON DATE_LITERAL SEMI
        TIME COLON TIME_LITERAL SEMI
      RBRACE
    ;

arrival
    : ARRIVAL LBRACE
        AIRPORT COLON airportCode SEMI
        DATE COLON DATE_LITERAL SEMI
        TIME COLON TIME_LITERAL SEMI
      RBRACE
    ;

fuel
    : FUEL LBRACE
        QUANTITY COLON positiveNumber unit SEMI
      RBRACE
    ;

profile
    : PROFILE LBRACE
        profileEntry+
      RBRACE
    ;

profileEntry
    : climbEntry
    | descendEntry
    ;

climbEntry
    : CLIMB LBRACKET
        ALTITUDE COLON positiveNumber unit COMMA
        SPEED COLON positiveNumber unit
      RBRACKET SEMI
    ;

descendEntry
    : DESCEND LBRACKET
        ALTITUDE COLON positiveNumber unit COMMA
        SPEED COLON positiveNumber unit COMMA
        RATE COLON signedNumber
      RBRACKET SEMI
    ;

segment
    : SEGMENT LBRACE
        MODE COLON segmentMode SEMI
        FROM COLON coord SEMI
        TO COLON coord SEMI
      RBRACE
    ;

segmentMode
    : CLIMB
    | CRUISE
    | DESCEND
    ;

coord
    : LPAREN signedNumber COMMA signedNumber COMMA ALTITUDE COLON positiveNumber unit RPAREN
    ;

airportCode
    : ICAO_CODE
    | IATA_CODE
    ;

signedNumber
    : MINUS? INT
    | MINUS? FLOAT
    ;

positiveNumber
    : INT
    | FLOAT
    ;

unit
    : KG
    | L
    | M
    | FT
    | MS
    | KTS
    ;

// LEXER RULES — Keywords (case-insensitive)

FLIGHT    : [Ff][Ll][Ii][Gg][Hh][Tt] ;
LEG       : [Ll][Ee][Gg] ;
DEPARTURE : [Dd][Ee][Pp][Aa][Rr][Tt][Uu][Rr][Ee] ;
ARRIVAL   : [Aa][Rr][Rr][Ii][Vv][Aa][Ll] ;
ROUTE     : [Rr][Oo][Uu][Tt][Ee] ;
SEGMENT   : [Ss][Ee][Gg][Mm][Ee][Nn][Tt] ;
FUEL      : [Ff][Uu][Ee][Ll] ;
PROFILE   : [Pp][Rr][Oo][Ff][Ii][Ll][Ee] ;
CLIMB     : [Cc][Ll][Ii][Mm][Bb] ;
CRUISE    : [Cc][Rr][Uu][Ii][Ss][Ee] ;
DESCEND   : [Dd][Ee][Ss][Cc][Ee][Nn][Dd] ;
TYPE      : [Tt][Yy][Pp][Ee] ;
AIRPORT   : [Aa][Ii][Rr][Pp][Oo][Rr][Tt] ;
DATE      : [Dd][Aa][Tt][Ee] ;
TIME      : [Tt][Ii][Mm][Ee] ;
QUANTITY  : [Qq][Uu][Aa][Nn][Tt][Ii][Tt][Yy] ;
ALTITUDE  : [Aa][Ll][Tt][Ii][Tt][Uu][Dd][Ee] ;
SPEED     : [Ss][Pp][Ee][Ee][Dd] ;
RATE      : [Rr][Aa][Tt][Ee] ;
MODE      : [Mm][Oo][Dd][Ee] ;
FROM      : [Ff][Rr][Oo][Mm] ;
TO        : [Tt][Oo] ;
REGULAR   : [Rr][Ee][Gg][Uu][Ll][Aa][Rr] ;
CHARTER   : [Cc][Hh][Aa][Rr][Tt][Ee][Rr] ;
AIRCRAFT  : [Aa][Ii][Rr][Cc][Rr][Aa][Ff][Tt] ;

// Units

KG  : 'kg' ;
L   : 'l' ;
M   : 'm' ;
FT  : 'ft' ;
MS  : 'm/s' ;
KTS : 'kts' ;

// Airport codes and identifiers

ICAO_CODE : [A-Z][A-Z][A-Z][A-Z] ;
IATA_CODE : [A-Z][A-Z][A-Z] ;
ID        : [A-Za-z][A-Za-z0-9]* ;

// Literals

DATE_LITERAL : [0-9][0-9][0-9][0-9] '-' [0-9][0-9] '-' [0-9][0-9] ;
TIME_LITERAL : [0-9][0-9] ':' [0-9][0-9] ;
FLOAT        : [0-9]+ '.' [0-9]+ ;
INT          : [0-9]+ ;

// Separators and symbols

LBRACE   : '{' ;
RBRACE   : '}' ;
LPAREN   : '(' ;
RPAREN   : ')' ;
LBRACKET : '[' ;
RBRACKET : ']' ;
COLON    : ':' ;
SEMI     : ';' ;
COMMA    : ',' ;
MINUS    : '-' ;

// Ignore whitespace and comments

WS      : [ \t\r\n]+ -> skip ;
COMMENT : '//' ~[\r\n]* -> skip ;