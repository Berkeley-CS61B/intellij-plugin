////////////////////////////////////////////////////////////////////////////////
// checkstyle: Checks Java source code for adherence to a set of rules.
// Copyright (C) 2011  Paul N. Hilfinger
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
////////////////////////////////////////////////////////////////////////////////
package ucb.checkstyle.checks;

import com.puppycrawl.tools.checkstyle.api.Check;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import com.puppycrawl.tools.checkstyle.api.TextBlock;

import org.apache.commons.beanutils.ConversionException;

import java.util.Map;
import java.util.List;
import java.util.Stack;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.regex.PatternSyntaxException;

import static java.util.Collections.sort;

/**
 * Checks that internal comments comply with configurable
 * restrictions.
 * @author P. N. Hilfinger
 */
public class InternalCommentsCheck extends Check
{
    /** List of regions of file, giving line numbers of the starts of
     *  methods, method bodies, classes, static initializers, and
     *  constructors.
     */
    private final List<Span> spans = new ArrayList<Span>();

    /** Stack of region types surrounding tokens currently being
     *  traversed. Needed to handle nested classes. */
    private final Stack<SpanType> context = new Stack<SpanType>();

    /** Default value for allowedCFormats and allowedCppFormats. */
    private final static String DEFAULT_ALLOWED_FORMATS =
        "package class func init prestmts stmts";

    /** Allowed formats and placements of C-style (/*) comments. Set by
     *  setAllowedCFormats. */
    private String allowedCFormats = DEFAULT_ALLOWED_FORMATS;
    /** Allowed formats and placements of C++-style (//) comments. Set by
     *  setAllowedCppFormats. */
    private String allowedCppFormats = DEFAULT_ALLOWED_FORMATS;
    /** Translation of allowedCFormats into a list of context/regexp pairs. */
    private List<AllowPattern> allowedCComments;
    /** Translation of allowedCppFormats into a list of context/regexp pairs. */
    private List<AllowPattern> allowedCppComments;

    /** Describe set of allowed places and formats for C-style comments.
     *  
     *  The possible values are concatenations of items with the format
     *  <pre>
     *      CONTEXT    
     *    or
     *      CONTEXT cREGEXPc
     *  </pre>
     *  where CONTEXT is one of 'package', 'class', 'func', 'init', 'prestmts',
     *  and 'stmts', c is any non-whitespace, non-word character that does not
     *  appear in REGEXP, a regular expression describing the allowed format of
     *  comments in the given context (default: no constraint).  The contexts
     *  describe possible locations in a source file: 'package' for everything
     *  outside any class; 'class' for anything in a class and outside a method,
     *  constructor, or static initializer; 'func' for anything in a method
     *  or constructor other than its body; 'init' for anything in a static 
     *  initializer up to its first statement; 'prestmts' for the space between 
     *  the opening "{" of a method or constructor and the first statement;
     *  and 'stmts' for the statement list of a method, constructor, or 
     *  static initializer.
     *
     *  @param formatDescriptor description of permissible contexts and formats.
     */
    public void setAllowedCFormats(String formatDescriptor) {
        allowedCFormats = formatDescriptor;
        allowedCComments = parseFormatDescriptor (formatDescriptor);
    }

    /** Describe set of allowed places and formats for C-style comments.
     *  
     *  The possible values are as described in setAllowedCFormats.
     *
     *  @param formatDescriptor description of permissible contexts and formats.
     */
    public void setAllowedCppFormats(String formatDescriptor) {
        allowedCppFormats = formatDescriptor;
        allowedCppComments = parseFormatDescriptor (formatDescriptor);
    }

    @Override
    public int[] getDefaultTokens()
    {
        return new int[] {TokenTypes.CLASS_DEF, TokenTypes.METHOD_DEF, 
                          TokenTypes.CTOR_DEF, TokenTypes.STATIC_INIT, };
    }

    @Override
    public void beginTree(DetailAST rootAST)
    {
        if (allowedCComments == null) 
            allowedCComments = parseFormatDescriptor (allowedCFormats);
        if (allowedCppComments == null) 
            allowedCppComments = parseFormatDescriptor (allowedCppFormats);
        spans.clear();
        context.clear();
        spans.add(new Span(SpanType.PACKAGE,1));
        context.push(SpanType.PACKAGE);
    }

    @Override
    public void finishTree(DetailAST ast) 
    {
        sort(spans);

        for (TextBlock text : getCCommentList()) {
            checkComment(text, "/*", allowedCComments);
        }
        for (TextBlock text : getCppCommentList()) {
            checkComment(text, "//", allowedCppComments);
        }
    }

    @Override
    public void visitToken(DetailAST ast)
    {
        int type = ast.getType();
        int line = ast.getLineNo();
        DetailAST body = ast.getLastChild();
        DetailAST stmt0 = body.getFirstChild();
        int bodyLine = body.getLineNo();
        int stmt0Line = stmt0 == null ? 0 : stmt0.getLineNo();
        int endLine = stmt0 == null ? 0 : body.getLastChild().getLineNo() + 1;
        if (endLine != 0)
            spans.add(new Span(context.peek(), endLine));
        switch (type) {
        case TokenTypes.CLASS_DEF:
            spans.add(new Span(SpanType.CLASS, line));
            context.push (SpanType.CLASS);
            break;
        case TokenTypes.METHOD_DEF: case TokenTypes.CTOR_DEF:
            if (stmt0 != null) {
                spans.add(new Span(SpanType.METHOD_OR_CTOR, line));
                spans.add(new Span(SpanType.PRESTMTS, bodyLine));
                spans.add(new Span(SpanType.STMTS, stmt0Line));
            }
            context.push (SpanType.STMTS);
            break;
        case TokenTypes.STATIC_INIT:
            spans.add(new Span(SpanType.STATIC_INIT, line));
            spans.add(new Span(SpanType.STMTS, stmt0Line));
            context.push (SpanType.STMTS);
            break;
        default:
            assert false : "unreachable statement";
        }
    }

    @Override
    public void leaveToken(DetailAST ast) {
        context.pop();
    }

    /**
     * Get the last span that begins at or before line.
     *
     * @param line line number to search for.
     * @return the last span at or before line line.
     */
    private Span getSpan(int line)
    {
        assert line > 0 && !spans.isEmpty();
        int low, high;
        low = 0;
        high = spans.size();
        while (low < high - 1) {
            int m = (low + high) / 2;
            if (line < spans.get(m).start) {
                high = m;
            } else {
                low = m;
            }
        }
        return spans.get(low);
    }
     
    /**
     * Get list of all C++-style comments in current file.
     *
     * @return list of TextBlocks containing text of all C++ comment
     * contents.
     */
    private List<TextBlock> getCppCommentList()
    {
        return new ArrayList<TextBlock>(getFileContents()
                                        .getCppComments().values());
    }

    /**
     * Get list of all C-style comments in current file.
     *
     * @return list of TextBlocks containing text of all C comment
     * contents.
     */
    private List<TextBlock> getCCommentList()
    {
        List<TextBlock> result = new ArrayList<TextBlock>();
        for (List<TextBlock> list : getFileContents()
                 .getCComments().values()) {
            result.addAll(result.size(), list);
        }
        return result;
    }

    /** Describes a single item in an argument to setAllowedCFormat or
     *  setAllowedCppFormat.  */
    private static final Pattern ELEMENT_PATTERN  = Pattern.compile
        ("\\G\\s*(class|package|init|func|(?:pre)?stmts)\\b\\s*"
         + "(?:([^\\w\\s])(.*?)\\2)?|\\s*(\\S)");

    /** Convert a string to a list of allowed context/regexp pairs.
     *
     * @param desc format descriptor as described for setAllowedCFormat.
     * @return corresponding list of context/regexp pairs.
     */
    private List<AllowPattern> parseFormatDescriptor(String desc)
    {
        List<AllowPattern> allowed = new ArrayList<AllowPattern>();
        Matcher inputs = ELEMENT_PATTERN.matcher(desc);
        allowed.clear();
        while (inputs.find()) {
            if (inputs.group(4) != null)
                throw new ConversionException("trailing garbage in format: '"
                                              + desc.substring(inputs.start(4))
                                              + "'");
            String place = inputs.group(1);
            String re = inputs.group(3);
            SpanType type;
            if (place.equals("class"))
                type = SpanType.CLASS;
            else if (place.equals("package"))
                type = SpanType.PACKAGE;
            else if (place.equals("init"))
                type = SpanType.STATIC_INIT;
            else if (place.equals("func"))
                type = SpanType.METHOD_OR_CTOR;
            else if (place.equals("prestmts"))
                type = SpanType.PRESTMTS;
            else
                type = SpanType.STMTS;
            try {
                if (re == null)
                    allowed.add(new AllowPattern(type, null));
                else
                    allowed.add(new AllowPattern(type, 
                        Pattern.compile(re, Pattern.DOTALL)));
            } catch (PatternSyntaxException e) {
                throw new ConversionException("bad format: '" + re + "'");
            }
        }
        return allowed;
    }        

    /** Check that a comment is allowed according to a list of allowed 
     *  context/format pairs, and log if it is not.
     *
     * @param comment comment to check.
     * @param id a string used in error messages to identify the kind of 
     *        comment.
     * @param allowed list of allowed context/pattern pairs.
     */
    private void checkComment(TextBlock comment, String id,
                              List<AllowPattern> allowed) 
    {
        int startLine = comment.getStartLineNo();
        int startCol = comment.getStartColNo();

        Span context = getSpan(startLine);

        if (allowed.isEmpty())
            log(startLine, startCol, "comment.neverAllowed", id);
        else {
            for (AllowPattern desc : allowed) {
                if (desc.type == context.type) {
                    if (desc.patn != null) {
                        StringBuffer text = new StringBuffer();
                        for (String line :  comment.getText()) {
                            text.append(line);
                            text.append("\n");
                        }
                        String comment1 = text.toString();
                        if (!desc.patn.matcher(comment1).matches()) {
                            log(startLine, startCol, "comment.badFormat", id);
                        }
                    }
                    return;
                }
            }
            log(startLine, startCol, "comment.notAllowed", id);
        }
    }

    /** The possible contexts of text in a file. */
    private static enum SpanType
    { 
        /** Outside of any class. */
        PACKAGE, 
        /** Within a class, outside of any method, constructor, or
         *  static initializer. */
        CLASS,
        /** Within a method or constructor before the opening "{" of its body. */
        METHOD_OR_CTOR, 
        /** The from the beginning of a static initializer to its
         *  first statement. */
        STATIC_INIT, 
        /** From the opening "{" of a method or constructor to its
         *  first statement. */
        PRESTMTS, 
        /** From the first statement to the end of the body of a
         *  method, constructor, or static initializer. */
        STMTS;
    }

    /** The beginning point of a region of text, indicating its
     *  type, as indicated by a SpanType. */
    private static class Span implements Comparable<Span>
    {
        /**
         * Describe a region of type starting at line start.
         *
         * @param type0 type of this region (method, class, body,
         * etc.).
         * @param start0 starting line number of region.
         */
        Span(SpanType type0, int start0) {
            type = type0;
            start = start0;
        }

        /* @Override */
        public int compareTo(Span x) {
            return start - x.start;
        }

        final SpanType type;
        final int start;
    }

    /** Describes a context and the comments allowed there. */
    private static class AllowPattern {
        AllowPattern(SpanType type0, Pattern patn0) {
            type = type0;
            patn = patn0;
        }

        SpanType type;
        Pattern patn;
    }

}
