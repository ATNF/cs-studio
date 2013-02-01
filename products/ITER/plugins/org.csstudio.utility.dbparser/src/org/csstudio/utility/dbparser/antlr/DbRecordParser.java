// $ANTLR 3.4 antlr/DbRecord.g 2013-01-28 12:45:10

package org.csstudio.utility.dbparser.antlr;


import org.antlr.runtime.*;
import java.util.Stack;
import java.util.List;
import java.util.ArrayList;

import org.antlr.runtime.tree.*;


@SuppressWarnings({"all", "warnings", "unchecked"})
public class DbRecordParser extends Parser {
    public static final String[] tokenNames = new String[] {
        "<invalid>", "<EOR>", "<DOWN>", "<UP>", "ALIAS", "COMMENT", "ESC_SEQ", "EXPONENT", "FIELD", "FLOAT", "HEX_DIGIT", "ID", "INFO", "INT", "OCTAL_ESC", "RECORD", "RECORD_BODY", "RECORD_INSTANCE", "String", "TYPE", "UNICODE_ESC", "VALUE", "WHITESPACE", "'('", "')'", "','", "'{'", "'}'"
    };

    public static final int EOF=-1;
    public static final int T__23=23;
    public static final int T__24=24;
    public static final int T__25=25;
    public static final int T__26=26;
    public static final int T__27=27;
    public static final int ALIAS=4;
    public static final int COMMENT=5;
    public static final int ESC_SEQ=6;
    public static final int EXPONENT=7;
    public static final int FIELD=8;
    public static final int FLOAT=9;
    public static final int HEX_DIGIT=10;
    public static final int ID=11;
    public static final int INFO=12;
    public static final int INT=13;
    public static final int OCTAL_ESC=14;
    public static final int RECORD=15;
    public static final int RECORD_BODY=16;
    public static final int RECORD_INSTANCE=17;
    public static final int String=18;
    public static final int TYPE=19;
    public static final int UNICODE_ESC=20;
    public static final int VALUE=21;
    public static final int WHITESPACE=22;

    // delegates
    public Parser[] getDelegates() {
        return new Parser[] {};
    }

    // delegators


    public DbRecordParser(TokenStream input) {
        this(input, new RecognizerSharedState());
    }
    public DbRecordParser(TokenStream input, RecognizerSharedState state) {
        super(input, state);
    }

protected TreeAdaptor adaptor = new CommonTreeAdaptor();

public void setTreeAdaptor(TreeAdaptor adaptor) {
    this.adaptor = adaptor;
}
public TreeAdaptor getTreeAdaptor() {
    return adaptor;
}
    public String[] getTokenNames() { return DbRecordParser.tokenNames; }
    public String getGrammarFileName() { return "antlr/DbRecord.g"; }


    public static class top_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "top"
    // antlr/DbRecord.g:27:1: top : program ;
    public final DbRecordParser.top_return top() throws RecognitionException {
        DbRecordParser.top_return retval = new DbRecordParser.top_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        DbRecordParser.program_return program1 =null;



        try {
            // antlr/DbRecord.g:27:5: ( program )
            // antlr/DbRecord.g:27:7: program
            {
            root_0 = (Object)adaptor.nil();


            pushFollow(FOLLOW_program_in_top125);
            program1=program();

            state._fsp--;

            adaptor.addChild(root_0, program1.getTree());

            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "top"


    public static class program_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "program"
    // antlr/DbRecord.g:29:1: program : ( record )* ;
    public final DbRecordParser.program_return program() throws RecognitionException {
        DbRecordParser.program_return retval = new DbRecordParser.program_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        DbRecordParser.record_return record2 =null;



        try {
            // antlr/DbRecord.g:29:9: ( ( record )* )
            // antlr/DbRecord.g:29:11: ( record )*
            {
            root_0 = (Object)adaptor.nil();


            // antlr/DbRecord.g:29:11: ( record )*
            loop1:
            do {
                int alt1=2;
                int LA1_0 = input.LA(1);

                if ( (LA1_0==RECORD) ) {
                    alt1=1;
                }


                switch (alt1) {
            	case 1 :
            	    // antlr/DbRecord.g:29:11: record
            	    {
            	    pushFollow(FOLLOW_record_in_program133);
            	    record2=record();

            	    state._fsp--;

            	    adaptor.addChild(root_0, record2.getTree());

            	    }
            	    break;

            	default :
            	    break loop1;
                }
            } while (true);


            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "program"


    public static class record_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "record"
    // antlr/DbRecord.g:31:1: record : record_head record_block -> ^( 'record_instance' record_head record_block ) ;
    public final DbRecordParser.record_return record() throws RecognitionException {
        DbRecordParser.record_return retval = new DbRecordParser.record_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        DbRecordParser.record_head_return record_head3 =null;

        DbRecordParser.record_block_return record_block4 =null;


        RewriteRuleSubtreeStream stream_record_block=new RewriteRuleSubtreeStream(adaptor,"rule record_block");
        RewriteRuleSubtreeStream stream_record_head=new RewriteRuleSubtreeStream(adaptor,"rule record_head");
        try {
            // antlr/DbRecord.g:31:9: ( record_head record_block -> ^( 'record_instance' record_head record_block ) )
            // antlr/DbRecord.g:31:11: record_head record_block
            {
            pushFollow(FOLLOW_record_head_in_record143);
            record_head3=record_head();

            state._fsp--;

            stream_record_head.add(record_head3.getTree());

            pushFollow(FOLLOW_record_block_in_record145);
            record_block4=record_block();

            state._fsp--;

            stream_record_block.add(record_block4.getTree());

            // AST REWRITE
            // elements: record_head, RECORD_INSTANCE, record_block
            // token labels: 
            // rule labels: retval
            // token list labels: 
            // rule list labels: 
            // wildcard labels: 
            retval.tree = root_0;
            RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.tree:null);

            root_0 = (Object)adaptor.nil();
            // 31:36: -> ^( 'record_instance' record_head record_block )
            {
                // antlr/DbRecord.g:31:39: ^( 'record_instance' record_head record_block )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(
                (Object)adaptor.create(RECORD_INSTANCE, "RECORD_INSTANCE")
                , root_1);

                adaptor.addChild(root_1, stream_record_head.nextTree());

                adaptor.addChild(root_1, stream_record_block.nextTree());

                adaptor.addChild(root_0, root_1);
                }

            }


            retval.tree = root_0;

            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "record"


    public static class record_head_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "record_head"
    // antlr/DbRecord.g:33:1: record_head : 'record' '(' key_value ')' -> ^( RECORD key_value ) ;
    public final DbRecordParser.record_head_return record_head() throws RecognitionException {
        DbRecordParser.record_head_return retval = new DbRecordParser.record_head_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token string_literal5=null;
        Token char_literal6=null;
        Token char_literal8=null;
        DbRecordParser.key_value_return key_value7 =null;


        Object string_literal5_tree=null;
        Object char_literal6_tree=null;
        Object char_literal8_tree=null;
        RewriteRuleTokenStream stream_RECORD=new RewriteRuleTokenStream(adaptor,"token RECORD");
        RewriteRuleTokenStream stream_23=new RewriteRuleTokenStream(adaptor,"token 23");
        RewriteRuleTokenStream stream_24=new RewriteRuleTokenStream(adaptor,"token 24");
        RewriteRuleSubtreeStream stream_key_value=new RewriteRuleSubtreeStream(adaptor,"rule key_value");
        try {
            // antlr/DbRecord.g:33:13: ( 'record' '(' key_value ')' -> ^( RECORD key_value ) )
            // antlr/DbRecord.g:33:15: 'record' '(' key_value ')'
            {
            string_literal5=(Token)match(input,RECORD,FOLLOW_RECORD_in_record_head163);  
            stream_RECORD.add(string_literal5);


            char_literal6=(Token)match(input,23,FOLLOW_23_in_record_head165);  
            stream_23.add(char_literal6);


            pushFollow(FOLLOW_key_value_in_record_head167);
            key_value7=key_value();

            state._fsp--;

            stream_key_value.add(key_value7.getTree());

            char_literal8=(Token)match(input,24,FOLLOW_24_in_record_head169);  
            stream_24.add(char_literal8);


            // AST REWRITE
            // elements: key_value
            // token labels: 
            // rule labels: retval
            // token list labels: 
            // rule list labels: 
            // wildcard labels: 
            retval.tree = root_0;
            RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.tree:null);

            root_0 = (Object)adaptor.nil();
            // 33:42: -> ^( RECORD key_value )
            {
                // antlr/DbRecord.g:33:45: ^( RECORD key_value )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(
                (Object)adaptor.create(RECORD, "RECORD")
                , root_1);

                adaptor.addChild(root_1, stream_key_value.nextTree());

                adaptor.addChild(root_0, root_1);
                }

            }


            retval.tree = root_0;

            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "record_head"


    public static class record_block_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "record_block"
    // antlr/DbRecord.g:35:1: record_block : '{' ( record_body )* '}' -> ^( 'record_body' ( record_body )* ) ;
    public final DbRecordParser.record_block_return record_block() throws RecognitionException {
        DbRecordParser.record_block_return retval = new DbRecordParser.record_block_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token char_literal9=null;
        Token char_literal11=null;
        DbRecordParser.record_body_return record_body10 =null;


        Object char_literal9_tree=null;
        Object char_literal11_tree=null;
        RewriteRuleTokenStream stream_26=new RewriteRuleTokenStream(adaptor,"token 26");
        RewriteRuleTokenStream stream_27=new RewriteRuleTokenStream(adaptor,"token 27");
        RewriteRuleSubtreeStream stream_record_body=new RewriteRuleSubtreeStream(adaptor,"rule record_body");
        try {
            // antlr/DbRecord.g:35:14: ( '{' ( record_body )* '}' -> ^( 'record_body' ( record_body )* ) )
            // antlr/DbRecord.g:35:16: '{' ( record_body )* '}'
            {
            char_literal9=(Token)match(input,26,FOLLOW_26_in_record_block185);  
            stream_26.add(char_literal9);


            // antlr/DbRecord.g:35:20: ( record_body )*
            loop2:
            do {
                int alt2=2;
                int LA2_0 = input.LA(1);

                if ( (LA2_0==ALIAS||LA2_0==FIELD||LA2_0==INFO) ) {
                    alt2=1;
                }


                switch (alt2) {
            	case 1 :
            	    // antlr/DbRecord.g:35:20: record_body
            	    {
            	    pushFollow(FOLLOW_record_body_in_record_block187);
            	    record_body10=record_body();

            	    state._fsp--;

            	    stream_record_body.add(record_body10.getTree());

            	    }
            	    break;

            	default :
            	    break loop2;
                }
            } while (true);


            char_literal11=(Token)match(input,27,FOLLOW_27_in_record_block190);  
            stream_27.add(char_literal11);


            // AST REWRITE
            // elements: record_body, RECORD_BODY
            // token labels: 
            // rule labels: retval
            // token list labels: 
            // rule list labels: 
            // wildcard labels: 
            retval.tree = root_0;
            RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.tree:null);

            root_0 = (Object)adaptor.nil();
            // 35:38: -> ^( 'record_body' ( record_body )* )
            {
                // antlr/DbRecord.g:35:41: ^( 'record_body' ( record_body )* )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(
                (Object)adaptor.create(RECORD_BODY, "RECORD_BODY")
                , root_1);

                // antlr/DbRecord.g:35:57: ( record_body )*
                while ( stream_record_body.hasNext() ) {
                    adaptor.addChild(root_1, stream_record_body.nextTree());

                }
                stream_record_body.reset();

                adaptor.addChild(root_0, root_1);
                }

            }


            retval.tree = root_0;

            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "record_block"


    public static class record_body_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "record_body"
    // antlr/DbRecord.g:37:1: record_body : ( field | info | alias );
    public final DbRecordParser.record_body_return record_body() throws RecognitionException {
        DbRecordParser.record_body_return retval = new DbRecordParser.record_body_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        DbRecordParser.field_return field12 =null;

        DbRecordParser.info_return info13 =null;

        DbRecordParser.alias_return alias14 =null;



        try {
            // antlr/DbRecord.g:37:13: ( field | info | alias )
            int alt3=3;
            switch ( input.LA(1) ) {
            case FIELD:
                {
                alt3=1;
                }
                break;
            case INFO:
                {
                alt3=2;
                }
                break;
            case ALIAS:
                {
                alt3=3;
                }
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("", 3, 0, input);

                throw nvae;

            }

            switch (alt3) {
                case 1 :
                    // antlr/DbRecord.g:37:15: field
                    {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_field_in_record_body208);
                    field12=field();

                    state._fsp--;

                    adaptor.addChild(root_0, field12.getTree());

                    }
                    break;
                case 2 :
                    // antlr/DbRecord.g:37:23: info
                    {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_info_in_record_body212);
                    info13=info();

                    state._fsp--;

                    adaptor.addChild(root_0, info13.getTree());

                    }
                    break;
                case 3 :
                    // antlr/DbRecord.g:37:30: alias
                    {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_alias_in_record_body216);
                    alias14=alias();

                    state._fsp--;

                    adaptor.addChild(root_0, alias14.getTree());

                    }
                    break;

            }
            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "record_body"


    public static class field_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "field"
    // antlr/DbRecord.g:39:1: field : 'field' '(' key_value ')' -> ^( FIELD key_value ) ;
    public final DbRecordParser.field_return field() throws RecognitionException {
        DbRecordParser.field_return retval = new DbRecordParser.field_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token string_literal15=null;
        Token char_literal16=null;
        Token char_literal18=null;
        DbRecordParser.key_value_return key_value17 =null;


        Object string_literal15_tree=null;
        Object char_literal16_tree=null;
        Object char_literal18_tree=null;
        RewriteRuleTokenStream stream_FIELD=new RewriteRuleTokenStream(adaptor,"token FIELD");
        RewriteRuleTokenStream stream_23=new RewriteRuleTokenStream(adaptor,"token 23");
        RewriteRuleTokenStream stream_24=new RewriteRuleTokenStream(adaptor,"token 24");
        RewriteRuleSubtreeStream stream_key_value=new RewriteRuleSubtreeStream(adaptor,"rule key_value");
        try {
            // antlr/DbRecord.g:39:7: ( 'field' '(' key_value ')' -> ^( FIELD key_value ) )
            // antlr/DbRecord.g:39:9: 'field' '(' key_value ')'
            {
            string_literal15=(Token)match(input,FIELD,FOLLOW_FIELD_in_field224);  
            stream_FIELD.add(string_literal15);


            char_literal16=(Token)match(input,23,FOLLOW_23_in_field226);  
            stream_23.add(char_literal16);


            pushFollow(FOLLOW_key_value_in_field228);
            key_value17=key_value();

            state._fsp--;

            stream_key_value.add(key_value17.getTree());

            char_literal18=(Token)match(input,24,FOLLOW_24_in_field230);  
            stream_24.add(char_literal18);


            // AST REWRITE
            // elements: key_value
            // token labels: 
            // rule labels: retval
            // token list labels: 
            // rule list labels: 
            // wildcard labels: 
            retval.tree = root_0;
            RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.tree:null);

            root_0 = (Object)adaptor.nil();
            // 39:35: -> ^( FIELD key_value )
            {
                // antlr/DbRecord.g:39:38: ^( FIELD key_value )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(
                (Object)adaptor.create(FIELD, "FIELD")
                , root_1);

                adaptor.addChild(root_1, stream_key_value.nextTree());

                adaptor.addChild(root_0, root_1);
                }

            }


            retval.tree = root_0;

            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "field"


    public static class info_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "info"
    // antlr/DbRecord.g:41:1: info : 'info' '(' key_value ')' -> ^( INFO key_value ) ;
    public final DbRecordParser.info_return info() throws RecognitionException {
        DbRecordParser.info_return retval = new DbRecordParser.info_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token string_literal19=null;
        Token char_literal20=null;
        Token char_literal22=null;
        DbRecordParser.key_value_return key_value21 =null;


        Object string_literal19_tree=null;
        Object char_literal20_tree=null;
        Object char_literal22_tree=null;
        RewriteRuleTokenStream stream_23=new RewriteRuleTokenStream(adaptor,"token 23");
        RewriteRuleTokenStream stream_24=new RewriteRuleTokenStream(adaptor,"token 24");
        RewriteRuleTokenStream stream_INFO=new RewriteRuleTokenStream(adaptor,"token INFO");
        RewriteRuleSubtreeStream stream_key_value=new RewriteRuleSubtreeStream(adaptor,"rule key_value");
        try {
            // antlr/DbRecord.g:41:6: ( 'info' '(' key_value ')' -> ^( INFO key_value ) )
            // antlr/DbRecord.g:41:8: 'info' '(' key_value ')'
            {
            string_literal19=(Token)match(input,INFO,FOLLOW_INFO_in_info246);  
            stream_INFO.add(string_literal19);


            char_literal20=(Token)match(input,23,FOLLOW_23_in_info248);  
            stream_23.add(char_literal20);


            pushFollow(FOLLOW_key_value_in_info250);
            key_value21=key_value();

            state._fsp--;

            stream_key_value.add(key_value21.getTree());

            char_literal22=(Token)match(input,24,FOLLOW_24_in_info252);  
            stream_24.add(char_literal22);


            // AST REWRITE
            // elements: key_value
            // token labels: 
            // rule labels: retval
            // token list labels: 
            // rule list labels: 
            // wildcard labels: 
            retval.tree = root_0;
            RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.tree:null);

            root_0 = (Object)adaptor.nil();
            // 41:33: -> ^( INFO key_value )
            {
                // antlr/DbRecord.g:41:36: ^( INFO key_value )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(
                (Object)adaptor.create(INFO, "INFO")
                , root_1);

                adaptor.addChild(root_1, stream_key_value.nextTree());

                adaptor.addChild(root_0, root_1);
                }

            }


            retval.tree = root_0;

            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "info"


    public static class alias_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "alias"
    // antlr/DbRecord.g:43:1: alias : 'alias' '(' type ')' -> ^( ALIAS type ) ;
    public final DbRecordParser.alias_return alias() throws RecognitionException {
        DbRecordParser.alias_return retval = new DbRecordParser.alias_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token string_literal23=null;
        Token char_literal24=null;
        Token char_literal26=null;
        DbRecordParser.type_return type25 =null;


        Object string_literal23_tree=null;
        Object char_literal24_tree=null;
        Object char_literal26_tree=null;
        RewriteRuleTokenStream stream_23=new RewriteRuleTokenStream(adaptor,"token 23");
        RewriteRuleTokenStream stream_24=new RewriteRuleTokenStream(adaptor,"token 24");
        RewriteRuleTokenStream stream_ALIAS=new RewriteRuleTokenStream(adaptor,"token ALIAS");
        RewriteRuleSubtreeStream stream_type=new RewriteRuleSubtreeStream(adaptor,"rule type");
        try {
            // antlr/DbRecord.g:43:7: ( 'alias' '(' type ')' -> ^( ALIAS type ) )
            // antlr/DbRecord.g:43:9: 'alias' '(' type ')'
            {
            string_literal23=(Token)match(input,ALIAS,FOLLOW_ALIAS_in_alias268);  
            stream_ALIAS.add(string_literal23);


            char_literal24=(Token)match(input,23,FOLLOW_23_in_alias270);  
            stream_23.add(char_literal24);


            pushFollow(FOLLOW_type_in_alias272);
            type25=type();

            state._fsp--;

            stream_type.add(type25.getTree());

            char_literal26=(Token)match(input,24,FOLLOW_24_in_alias274);  
            stream_24.add(char_literal26);


            // AST REWRITE
            // elements: type
            // token labels: 
            // rule labels: retval
            // token list labels: 
            // rule list labels: 
            // wildcard labels: 
            retval.tree = root_0;
            RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.tree:null);

            root_0 = (Object)adaptor.nil();
            // 43:30: -> ^( ALIAS type )
            {
                // antlr/DbRecord.g:43:33: ^( ALIAS type )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(
                (Object)adaptor.create(ALIAS, "ALIAS")
                , root_1);

                adaptor.addChild(root_1, stream_type.nextTree());

                adaptor.addChild(root_0, root_1);
                }

            }


            retval.tree = root_0;

            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "alias"


    public static class key_value_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "key_value"
    // antlr/DbRecord.g:45:1: key_value : type ',' value -> ^( TYPE type ) ^( VALUE value ) ;
    public final DbRecordParser.key_value_return key_value() throws RecognitionException {
        DbRecordParser.key_value_return retval = new DbRecordParser.key_value_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token char_literal28=null;
        DbRecordParser.type_return type27 =null;

        DbRecordParser.value_return value29 =null;


        Object char_literal28_tree=null;
        RewriteRuleTokenStream stream_25=new RewriteRuleTokenStream(adaptor,"token 25");
        RewriteRuleSubtreeStream stream_value=new RewriteRuleSubtreeStream(adaptor,"rule value");
        RewriteRuleSubtreeStream stream_type=new RewriteRuleSubtreeStream(adaptor,"rule type");
        try {
            // antlr/DbRecord.g:45:11: ( type ',' value -> ^( TYPE type ) ^( VALUE value ) )
            // antlr/DbRecord.g:45:13: type ',' value
            {
            pushFollow(FOLLOW_type_in_key_value290);
            type27=type();

            state._fsp--;

            stream_type.add(type27.getTree());

            char_literal28=(Token)match(input,25,FOLLOW_25_in_key_value292);  
            stream_25.add(char_literal28);


            pushFollow(FOLLOW_value_in_key_value294);
            value29=value();

            state._fsp--;

            stream_value.add(value29.getTree());

            // AST REWRITE
            // elements: value, type
            // token labels: 
            // rule labels: retval
            // token list labels: 
            // rule list labels: 
            // wildcard labels: 
            retval.tree = root_0;
            RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.tree:null);

            root_0 = (Object)adaptor.nil();
            // 45:28: -> ^( TYPE type ) ^( VALUE value )
            {
                // antlr/DbRecord.g:45:31: ^( TYPE type )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(
                (Object)adaptor.create(TYPE, "TYPE")
                , root_1);

                adaptor.addChild(root_1, stream_type.nextTree());

                adaptor.addChild(root_0, root_1);
                }

                // antlr/DbRecord.g:45:44: ^( VALUE value )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(
                (Object)adaptor.create(VALUE, "VALUE")
                , root_1);

                adaptor.addChild(root_1, stream_value.nextTree());

                adaptor.addChild(root_0, root_1);
                }

            }


            retval.tree = root_0;

            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "key_value"


    public static class type_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "type"
    // antlr/DbRecord.g:47:1: type : ID ;
    public final DbRecordParser.type_return type() throws RecognitionException {
        DbRecordParser.type_return retval = new DbRecordParser.type_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token ID30=null;

        Object ID30_tree=null;

        try {
            // antlr/DbRecord.g:47:6: ( ID )
            // antlr/DbRecord.g:47:8: ID
            {
            root_0 = (Object)adaptor.nil();


            ID30=(Token)match(input,ID,FOLLOW_ID_in_type316); 
            ID30_tree = 
            (Object)adaptor.create(ID30)
            ;
            adaptor.addChild(root_0, ID30_tree);


            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "type"


    public static class value_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "value"
    // antlr/DbRecord.g:49:1: value : String ;
    public final DbRecordParser.value_return value() throws RecognitionException {
        DbRecordParser.value_return retval = new DbRecordParser.value_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token String31=null;

        Object String31_tree=null;

        try {
            // antlr/DbRecord.g:49:7: ( String )
            // antlr/DbRecord.g:49:9: String
            {
            root_0 = (Object)adaptor.nil();


            String31=(Token)match(input,String,FOLLOW_String_in_value324); 
            String31_tree = 
            (Object)adaptor.create(String31)
            ;
            adaptor.addChild(root_0, String31_tree);


            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "value"

    // Delegated rules


 

    public static final BitSet FOLLOW_program_in_top125 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_record_in_program133 = new BitSet(new long[]{0x0000000000008002L});
    public static final BitSet FOLLOW_record_head_in_record143 = new BitSet(new long[]{0x0000000004000000L});
    public static final BitSet FOLLOW_record_block_in_record145 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_RECORD_in_record_head163 = new BitSet(new long[]{0x0000000000800000L});
    public static final BitSet FOLLOW_23_in_record_head165 = new BitSet(new long[]{0x0000000000000800L});
    public static final BitSet FOLLOW_key_value_in_record_head167 = new BitSet(new long[]{0x0000000001000000L});
    public static final BitSet FOLLOW_24_in_record_head169 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_26_in_record_block185 = new BitSet(new long[]{0x0000000008001110L});
    public static final BitSet FOLLOW_record_body_in_record_block187 = new BitSet(new long[]{0x0000000008001110L});
    public static final BitSet FOLLOW_27_in_record_block190 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_field_in_record_body208 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_info_in_record_body212 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_alias_in_record_body216 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_FIELD_in_field224 = new BitSet(new long[]{0x0000000000800000L});
    public static final BitSet FOLLOW_23_in_field226 = new BitSet(new long[]{0x0000000000000800L});
    public static final BitSet FOLLOW_key_value_in_field228 = new BitSet(new long[]{0x0000000001000000L});
    public static final BitSet FOLLOW_24_in_field230 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_INFO_in_info246 = new BitSet(new long[]{0x0000000000800000L});
    public static final BitSet FOLLOW_23_in_info248 = new BitSet(new long[]{0x0000000000000800L});
    public static final BitSet FOLLOW_key_value_in_info250 = new BitSet(new long[]{0x0000000001000000L});
    public static final BitSet FOLLOW_24_in_info252 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ALIAS_in_alias268 = new BitSet(new long[]{0x0000000000800000L});
    public static final BitSet FOLLOW_23_in_alias270 = new BitSet(new long[]{0x0000000000000800L});
    public static final BitSet FOLLOW_type_in_alias272 = new BitSet(new long[]{0x0000000001000000L});
    public static final BitSet FOLLOW_24_in_alias274 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_type_in_key_value290 = new BitSet(new long[]{0x0000000002000000L});
    public static final BitSet FOLLOW_25_in_key_value292 = new BitSet(new long[]{0x0000000000040000L});
    public static final BitSet FOLLOW_value_in_key_value294 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ID_in_type316 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_String_in_value324 = new BitSet(new long[]{0x0000000000000002L});

}