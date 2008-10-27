/**
 * Fabric3
 * Copyright � 2008 Metaform Systems Limited
 *
 * The Antlr grammar for the domain admin interpreter.
 */
grammar DomainAdmin;

options {
output=AST;
ASTLabelType=CommonTree;
}

tokens {  INSTALL_CMD;
	      DEPLOY_CMD;
	      AUTH_CMD;
	      LIST_CMD;
	      PARAMETER;
	      PARAM_USERNAME;
	      PARAM_PASSWORD;
	      FILE;
	      PARAM_CONTRIBUTION_NAME;
	      PARAM_PLAN_NAME;
	      PARAM_PLAN_FILE;
}

@header { package org.fabric3.admin.cli; }
@lexer::header { package org.fabric3.admin.cli; }

@members {

   protected void mismatch(IntStream input, int ttype, BitSet follow) throws RecognitionException {
      super.mismatch(input, ttype, follow);
      // throw new MismatchedTokenException(ttype, input);
   }

   public void recoverFromMismatchedSet(IntStream input, RecognitionException e, BitSet follow) throws RecognitionException {
      super.recoverFromMismatchedSet(input, e, follow);
      // throw e;
   }

}

// Alter code generation so catch-clauses get replace with this action.
@rulecatch {
   catch (RecognitionException e) {
      throw e;
   }
}

command		: (subcommand)+ EOF;

subcommand	: (install | deploy | auth | list) NEWLINE?;

// main commands
install 	    : INSTALL file WS? param* -> ^(INSTALL_CMD file param*);
auth            : AUTH auth_param auth_param -> ^(AUTH_CMD auth_param auth_param);
list            : LIST auth_param* -> ^(LIST_CMD auth_param*);
deploy 		    : DEPLOY contribution_name plan_name?  plan_file? WS? param* -> ^(DEPLOY_CMD contribution_name plan_name? plan_file? param*);
uninstall 	    : UNINSTALL contribution? WS? param*;
undeploy 	    : UNDEPLOY contribution WS? param+;

file		    : STRING -> ^(FILE STRING);
param		    : operator STRING WS? -> ^(PARAMETER operator STRING) ;
auth_param	    : auth_operator STRING WS? -> ^(PARAMETER auth_operator STRING) ;
operator        : (username | password | contribution);
auth_operator   : (username | password);
username        : USERNAME -> ^(PARAM_USERNAME);
password        : PASSWORD -> ^(PARAM_PASSWORD);
contribution    : CONTRIBUTION -> ^(PARAM_CONTRIBUTION_NAME);
contribution_name : STRING -> ^(PARAM_CONTRIBUTION_NAME STRING);
plan_name         : STRING -> ^(PARAM_PLAN_NAME STRING);
plan_file         : PLAN STRING -> ^(PARAM_PLAN_FILE STRING);

INSTALL 	    : ('install' | 'ins');
AUTH 	        : ('authenticate' | 'auth');
LIST     	    : ('list' | 'ls');
DEPLOY		    : ('deploy' | 'dep');
UNINSTALL 	    : ('uninstall' | 'uins');
UNDEPLOY	    : ('undeploy' | 'udep');
USERNAME	    : ('-u' | '-username' | '-USERNAME') ;
PASSWORD	    : ('-p' | '-P'|'-PASSWORD' | '-password');
CONTRIBUTION    : ('-n' | '-N'|'-NAME'| '-name');
PLAN            : ('-plan' | '-PLAN');

STRING	        : ('a'..'z'|'A'..'Z'|'0'..'9'|'.' |'-'|'_' | '/' | ':')+;
NEWLINE    	    : '\r'? '\n';
WS		        : (' '|'\t'|'\n'|'\r')+ {skip();};