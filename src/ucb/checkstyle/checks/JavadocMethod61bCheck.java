////////////////////////////////////////////////////////////////////////////////
// checkstyle: Checks Java source code for adherence to a set of rules.
// Copyright (C) 2001-2015 the original author or authors.
// Copyright (C) 2011-2015 P. N. Hilfinger (modifications).
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

import antlr.collections.AST;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.FileContents;
import com.puppycrawl.tools.checkstyle.api.FullIdent;
import com.puppycrawl.tools.checkstyle.api.Scope;
import com.puppycrawl.tools.checkstyle.api.TextBlock;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import com.puppycrawl.tools.checkstyle.checks.AbstractTypeAwareCheck;
import com.puppycrawl.tools.checkstyle.checks.javadoc.JavadocTag;
import com.puppycrawl.tools.checkstyle.checks.javadoc.JavadocTagInfo;
import com.puppycrawl.tools.checkstyle.utils.CheckUtils;
import com.puppycrawl.tools.checkstyle.utils.CommonUtils;
import com.puppycrawl.tools.checkstyle.utils.ScopeUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;


/**
 * Checks the Javadoc of a method or constructor.
 *
 * This version is an extension of the original JavadocMethodCheck by
 * Burn, Giles, and Sukhodoslky that permits a different way of
 * describing parameters:
 *   *  When the allowNarrativeParamTags flag is true, a parameter is
 *      considered to be mentioned if it appears in all uppercase in the
 *      text of the Javadoc comment, as well as when it appears as a
 *      @@param tag.  Default value: false.
 *   *  When the allowNarrativeReturnTags flag is true, a return value is
 *      taken to be described if any of the word "returns", "return",
 *      "returning", "yield", "yields", or "yielding" appears (case
 *      insensitively) in the text of the comment, as well as when it appears 
 *      in a @@return tag.  Default value: false.
 *   *  Any parameter whose name matches the regexp unusedParamFormat
 *      need not appear in the Javadoc comment. Default value: null,
 *      which matches nothing.
 *
 * @author Oliver Burn
 * @author Rick Giles
 * @author o_sukhodoslky
 * @author Paul Hilfinger
 * @version 1.0 (from JavadocMethodCheck in checkstyle-6.8.1)
 */
@SuppressWarnings("deprecation")
public class JavadocMethod61bCheck extends AbstractTypeAwareCheck {

    /**
     * A key is pointing to the warning message text in "messages.properties"
     * file.
     */
    public static final String MSG_JAVADOC_MISSING = "javadoc.missing";

    /**
     * A key is pointing to the warning message text in "messages.properties"
     * file.
     */
    public static final String MSG_CLASS_INFO = "javadoc.classInfo";

    /**
     * A key is pointing to the warning message text in "messages.properties"
     * file.
     */
    public static final String MSG_UNUSED_TAG_GENERAL = "javadoc.unusedTagGeneral";

    /**
     * A key is pointing to the warning message text in "messages.properties"
     * file.
     */
    public static final String MSG_INVALID_INHERIT_DOC = "javadoc.invalidInheritDoc";

    /**
     * A key is pointing to the warning message text in "messages.properties"
     * file.
     */
    public static final String MSG_UNUSED_TAG = "javadoc.unusedTag";

    /**
     * A key is pointing to the warning message text in "messages.properties"
     * file.
     */
    public static final String MSG_EXCPECTED_TAG = "javadoc.expectedTag";

    /**
     * A key is pointing to the warning message text in "messages.properties"
     * file.
     */
    public static final String MSG_RETURN_EXPECTED = "javadoc.return.expected";

    /**
     * A key is pointing to the warning message text in "messages.properties"
     * file.
     */
    public static final String MSG_DUPLICATE_TAG = "javadoc.duplicateTag";

    /** compiled regexp to match Javadoc tags that take an argument * */
    private static final Pattern MATCH_JAVADOC_ARG =
        CommonUtils.createPattern("@(throws|exception|param)\\s+(\\S+)\\s+\\S*");

    /** compiled regexp to match first part of multilineJavadoc tags * */
    private static final Pattern MATCH_JAVADOC_ARG_MULTILINE_START =
        CommonUtils.createPattern("@(throws|exception|param)\\s+(\\S+)\\s*$");

    /** compiled regexp to look for a continuation of the comment * */
    private static final Pattern MATCH_JAVADOC_MULTILINE_CONT =
        CommonUtils.createPattern("(\\*/|@|[^\\s\\*])");

    /** Multiline finished at end of comment * */
    private static final String END_JAVADOC = "*/";
    /** Multiline finished at next Javadoc * */
    private static final String NEXT_TAG = "@";

    /** compiled regexp to match Javadoc tags with no argument * */
    private static final Pattern MATCH_JAVADOC_NOARG =
        CommonUtils.createPattern("@(return|see)\\s+\\S");
    /** compiled regexp to match first part of multilineJavadoc tags * */
    private static final Pattern MATCH_JAVADOC_NOARG_MULTILINE_START =
        CommonUtils.createPattern("@(return|see)\\s*$");
    /** compiled regexp to match Javadoc tags with no argument and {} * */
    private static final Pattern MATCH_JAVADOC_NOARG_CURLY =
        CommonUtils.createPattern("\\{\\s*@(inheritDoc)\\s*\\}");

    /** Maximum children allowed * */
    private static final int MAX_CHILDREN = 7;

    /** Maximum children allowed * */
    private static final int BODY_SIZE = 3;

    /** Default value of minimal amount of lines in method to demand documentation presence.*/
    private static final int DEFAULT_MIN_LINE_COUNT = -1;

    /** the visibility scope where Javadoc comments are checked * */
    private Scope scope = Scope.PRIVATE;

    /** the visibility scope where Javadoc comments shouldn't be checked * */
    private Scope excludeScope;

    /** Minimal amount of lines in method to demand documentation presence.*/
    private int minLineCount = DEFAULT_MIN_LINE_COUNT;

    /**
     * controls whether to allow documented exceptions that are not declared if
     * they are a subclass of java.lang.RuntimeException.
     */
    private boolean allowUndeclaredRTE;

    /**
     * Allows validating throws tags.
     */
    private boolean validateThrows;

    /**
     * controls whether to allow documented exceptions that are subclass of one
     * of declared exception. Defaults to false (backward compatibility).
     */
    private boolean allowThrowsTagsForSubclasses;

    /**
     * controls whether to ignore errors when a method has parameters but does
     * not have matching param tags in the javadoc. Defaults to false.
     */
    private boolean allowMissingParamTags;

    /**
     * controls whether to ignore errors when a method declares that it throws
     * exceptions but does not have matching throws tags in the javadoc.
     * Defaults to false.
     */
    private boolean allowMissingThrowsTags;

    /**
     * controls whether to ignore errors when a method returns non-void type
     * but does not have a return tag in the javadoc. Defaults to false.
     */
    private boolean allowMissingReturnTag;

    /**
     * Controls whether to ignore errors when there is no javadoc. Defaults to
     * false.
     */
    private boolean allowMissingJavadoc;

    /**
     * Controls whether to allow missing Javadoc on accessor methods for
     * properties (setters and getters).
     */
    private boolean allowMissingPropertyJavadoc;

    /**
     * Controls whether to allow parameters to be described in running
     * text, written in all caps.
     */
    private boolean allowNarrativeParamTags;

    /**
     * Controls whether to allow return values of functions to be described
     * in running text, using the words "return", "returning", "returns",
     * "yield", "yields", or "yielding", in any case.
     */
    private boolean allowNarrativeReturnTags;

    /** Compiled regexp for unused parameters. */
    private Pattern unusedParamFormatRE;

    /**
     * The format of parameter names that need not be mentioned in a
     * Javadoc comment.
     */
    private String unusedParamFormat;

    /** List of annotations that could allow missed documentation. */
    private List<String> allowedAnnotations = Arrays.asList("Override");

    /** Method names that match this pattern do not require javadoc blocks. */
    private Pattern ignoreMethodNamesRegex;

    /**
     * Set regex for matching method names to ignore.
     * @param regex regex for matching method names.
     */
    public void setIgnoreMethodNamesRegex(String regex) {
        ignoreMethodNamesRegex = CommonUtils.createPattern(regex);
    }

    /**
     * Sets minimal amount of lines in method.
     * @param value user's value.
     */
    public void setMinLineCount(int value) {
        minLineCount = value;
    }

    /**
     * Allow validating throws tag.
     * @param value user's value.
     */
    public void setValidateThrows(boolean value) {
        validateThrows = value;
    }

    /**
     * Sets list of annotations.
     * @param userAnnotations user's value.
     */
    public void setAllowedAnnotations(String userAnnotations) {
        final List<String> annotations = new ArrayList<>();
        final String[] sAnnotations = userAnnotations.split(",");
        for (int i = 0; i < sAnnotations.length; i++) {
            sAnnotations[i] = sAnnotations[i].trim();
        }

        Collections.addAll(annotations, sAnnotations);
        allowedAnnotations = annotations;
    }

    /**
     * Set the scope.
     *
     * @param from a <code>String</code> value
     */
    public void setScope(String from) {
        scope = Scope.getInstance(from);
    }

    /**
     * Set the excludeScope.
     *
     * @param scope a <code>String</code> value
     */
    public void setExcludeScope(String scope) {
        excludeScope = Scope.getInstance(scope);
    }

    /**
     * controls whether to allow documented exceptions that are not declared if
     * they are a subclass of java.lang.RuntimeException.
     *
     * @param flag a <code>Boolean</code> value
     */
    public void setAllowUndeclaredRTE(boolean flag) {
        allowUndeclaredRTE = flag;
    }

    /**
     * controls whether to allow documented exception that are subclass of one
     * of declared exceptions.
     *
     * @param flag a <code>Boolean</code> value
     */
    public void setAllowThrowsTagsForSubclasses(boolean flag) {
        allowThrowsTagsForSubclasses = flag;
    }

    /**
     * controls whether to allow a method which has parameters to omit matching
     * param tags in the javadoc. Defaults to false.
     *
     * @param flag a <code>Boolean</code> value
     */
    public void setAllowMissingParamTags(boolean flag) {
        allowMissingParamTags = flag;
    }

    /**
     * controls whether to allow a method which declares that it throws
     * exceptions to omit matching throws tags in the javadoc. Defaults to
     * false.
     *
     * @param flag a <code>Boolean</code> value
     */
    public void setAllowMissingThrowsTags(boolean flag) {
        allowMissingThrowsTags = flag;
    }

    /**
     * controls whether to allow a method which returns non-void type to omit
     * the return tag in the javadoc. Defaults to false.
     *
     * @param flag a <code>Boolean</code> value
     */
    public void setAllowMissingReturnTag(boolean flag) {
        allowMissingReturnTag = flag;
    }

    /**
     * Controls whether to ignore errors when there is no javadoc. Defaults to
     * false.
     *
     * @param flag a <code>Boolean</code> value
     */
    public void setAllowMissingJavadoc(boolean flag) {
        allowMissingJavadoc = flag;
    }

    /**
     * Controls whether to ignore errors when there is no javadoc for a
     * property accessor (setter/getter methods). Defaults to false.
     *
     * @param flag a <code>Boolean</code> value
     */
    public void setAllowMissingPropertyJavadoc(final boolean flag) {
        allowMissingPropertyJavadoc = flag;
    }

    /**
     * Controls whether to allow descriptions of parameters to be
     * included in the main text of a Javadoc comment, written in all caps.
     * Defaults to false.
     *
     * @param aFlag a <code>Boolean</code> value
     */
    public void setAllowNarrativeParamTags(final boolean aFlag) {
        allowNarrativeParamTags = aFlag;
    }

    /**
     * Controls whether to allow return values of functions to be described
     * in running text, using the words "return", "returning", "returns",
     * "yield", "yields", or "yielding", in any case.
     * @param aFlag a <code>Boolean</code> value
     */
    public void setAllowNarrativeReturnTags(final boolean aFlag) {
        allowNarrativeReturnTags = aFlag;
    }

    /**
     * Set the format of parameter names that need not be commented.
     * Default value is null, matching no names.
     *
     * @param format a <code>String</code> value
     */
    public void setUnusedParamFormat(String format) {
        unusedParamFormat = format;
        unusedParamFormatRE = Pattern.compile(format);
    }

    @Override
    public int[] getDefaultTokens() {
        return new int[] {TokenTypes.PACKAGE_DEF, TokenTypes.IMPORT,
                          TokenTypes.CLASS_DEF, TokenTypes.ENUM_DEF,
                          TokenTypes.INTERFACE_DEF,
                          TokenTypes.METHOD_DEF, TokenTypes.CTOR_DEF,
                          TokenTypes.ANNOTATION_FIELD_DEF,
        };
    }

    @Override
    public int[] getAcceptableTokens() {
        return new int[] {TokenTypes.METHOD_DEF, TokenTypes.CTOR_DEF,
                          TokenTypes.ANNOTATION_FIELD_DEF,
        };
    }

    @Override
    public boolean isCommentNodesRequired() {
        return true;
    }

    @Override
    protected final void processAST(DetailAST ast) {
        if ((ast.getType() == TokenTypes.METHOD_DEF || ast.getType() == TokenTypes.CTOR_DEF)
            && getMethodsNumberOfLine(ast) <= minLineCount
            || hasAllowedAnnotations(ast)) {
            return;
        }
        final Scope theScope = calculateScope(ast);
        if (shouldCheck(ast, theScope)) {
            final FileContents contents = getFileContents();
            final TextBlock cmt = contents.getJavadocBefore(ast.getLineNo());

            if (cmt == null) {
                if (!isMissingJavadocAllowed(ast)) {
                    log(ast, MSG_JAVADOC_MISSING);
                }
            }
            else {
                checkComment(ast, cmt);
            }
        }
    }

    /**
     * Some javadoc.
     * @param methodDef Some javadoc.
     * @return Some javadoc.
     */
    private boolean hasAllowedAnnotations(DetailAST methodDef) {
        final DetailAST modifiersNode = methodDef.findFirstToken(TokenTypes.MODIFIERS);
        DetailAST annotationNode = modifiersNode.findFirstToken(TokenTypes.ANNOTATION);
        while (annotationNode != null && annotationNode.getType() == TokenTypes.ANNOTATION) {
            DetailAST identNode = annotationNode.findFirstToken(TokenTypes.IDENT);
            if (identNode == null) {
                identNode = annotationNode.findFirstToken(TokenTypes.DOT)
                    .findFirstToken(TokenTypes.IDENT);
            }
            if (allowedAnnotations.contains(identNode.getText())) {
                return true;
            }
            annotationNode = annotationNode.getNextSibling();
        }
        return false;
    }

    /**
     * Some javadoc.
     * @param methodDef Some javadoc.
     * @return Some javadoc.
     */
    private int getMethodsNumberOfLine(DetailAST methodDef) {
        int numberOfLines;
        final DetailAST lcurly = methodDef.getLastChild();
        final DetailAST rcurly = lcurly.getLastChild();

        if (lcurly.getFirstChild() == rcurly) {
            numberOfLines = 1;
        }
        else {
            numberOfLines = rcurly.getLineNo() - lcurly.getLineNo() - 1;
        }
        return numberOfLines;
    }

    @Override
    protected final void logLoadError(Token ident) {
        logLoadErrorImpl(ident.getLineNo(), ident.getColumnNo(),
            MSG_CLASS_INFO,
            JavadocTagInfo.THROWS.getText(), ident.getText());
    }

    /**
     * The JavadocMethodCheck is about to report a missing Javadoc.
     * This hook can be used by derived classes to allow a missing javadoc
     * in some situations.  The default implementation checks
     * <code>allowMissingJavadoc</code> and
     * <code>allowMissingPropertyJavadoc</code> properties, do not forget
     * to call <code>super.isMissingJavadocAllowed(ast)</code> in case
     * you want to keep this logic.
     * @param ast the tree node for the method or constructor.
     * @return True if this method or constructor doesn't need Javadoc.
     */
    protected boolean isMissingJavadocAllowed(final DetailAST ast) {
        return allowMissingJavadoc
            || allowMissingPropertyJavadoc
                && (isSetterMethod(ast) || isGetterMethod(ast))
            || matchesSkipRegex(ast);
    }

    /**
     * Checks if the given method name matches the regex. In that case
     * we skip enforcement of javadoc for this method
     * @param methodDef {@link TokenTypes#METHOD_DEF METHOD_DEF}
     * @return true if given method name matches the regex.
     */
    private boolean matchesSkipRegex(DetailAST methodDef) {
        if (ignoreMethodNamesRegex != null) {
            final DetailAST ident = methodDef.findFirstToken(TokenTypes.IDENT);
            final String methodName = ident.getText();

            final Matcher matcher = ignoreMethodNamesRegex.matcher(methodName);
            if (matcher.matches()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether we should check this node.
     *
     * @param ast a given node.
     * @param scope the scope of the node.
     * @return whether we should check a given node.
     */
    private boolean shouldCheck(final DetailAST ast, final Scope scope) {
        final Scope surroundingScope = ScopeUtils.getSurroundingScope(ast);

        return scope.isIn(this.scope)
                && surroundingScope.isIn(this.scope)
                && (excludeScope == null || !scope.isIn(excludeScope)
                    || !surroundingScope.isIn(excludeScope));
    }

    /**
     * Checks the Javadoc for a method.
     *
     * @param ast the token for the method
     * @param comment the Javadoc comment
     */
    private void checkComment(DetailAST ast, TextBlock comment) {
        final String[] commentLines = comment.getText();
        final int startLine = comment.getStartLineNo();
        final int startCol = comment.getStartColNo();
        final List<JavadocTag> tags =
            getMethodTags(commentLines, startLine, startCol);

        if (hasShortCircuitTag(ast, tags)) {
            return;
        }

        if (ast.getType() != TokenTypes.ANNOTATION_FIELD_DEF) {
            // Check for inheritDoc
            boolean hasInheritDocTag = false;
            for (JavadocTag jt : tags) {
                if (jt.isInheritDocTag()) {
                    hasInheritDocTag = true;
                    break;
                }
            }

            checkParams(tags, ast, commentLines, !hasInheritDocTag);
            checkThrowsTags(tags, getThrows(ast), !hasInheritDocTag);
            if (isFunction(ast)) {
                checkReturnTag(tags, ast.getLineNo(), commentLines,
                               !hasInheritDocTag);
            }
        }

        // Dump out all unused tags
        for (JavadocTag jt : tags) {
            if (!jt.isSeeOrInheritDocTag()) {
                log(jt.getLineNo(), MSG_UNUSED_TAG_GENERAL);
            }
        }
    }

    /**
     * Validates whether the Javadoc has a short circuit tag. Currently this is
     * the inheritTag. Any errors are logged.
     *
     * @param ast the construct being checked
     * @param tags the list of Javadoc tags associated with the construct
     * @return true if the construct has a short circuit tag.
     */
    private boolean hasShortCircuitTag(final DetailAST ast,
            final List<JavadocTag> tags) {
        // Check if it contains {@inheritDoc} tag
        if (tags.size() != 1
                || !tags.get(0).isInheritDocTag()) {
            return false;
        }

        // Invalid if private, a constructor, or a static method
        if (!JavadocTagInfo.INHERIT_DOC.isValidOn(ast)) {
            log(ast, MSG_INVALID_INHERIT_DOC);
        }

        return true;
    }

    /**
     * Returns the scope for the method/constructor at the specified AST. If
     * the method is in an interface or annotation block, the scope is assumed
     * to be public.
     *
     * @param ast the token of the method/constructor
     * @return the scope of the method/constructor
     */
    private Scope calculateScope(final DetailAST ast) {
        final DetailAST mods = ast.findFirstToken(TokenTypes.MODIFIERS);
        final Scope declaredScope = ScopeUtils.getScopeFromMods(mods);
        return ScopeUtils.isInInterfaceOrAnnotationBlock(ast) ? Scope.PUBLIC
                : declaredScope;
    }

    /**
     * If we have a match for a parameter tag, add to aTags and return true.
     *
     * @param aTags list of tags to modify.
     * @param aLines lines of comment.
     * @param aIndex index of current line in aLines.
     * @param aTagPattern pattern for tag.
     * @param aCurrentLine current source line number.
     * @param aStartCol starting column of first line in aLines.
     * @return True iff match for aTagPattern found.
     */
    private boolean addSimpleMethodTag(final List<JavadocTag> aTags,
                                       final String[] aLines,
                                       int aIndex,
                                       final Pattern aTagPattern,
                                       int aCurrentLine,
                                       int aStartCol) {
        final Matcher matcher = aTagPattern.matcher(aLines[aIndex]);
        if (matcher.find()) {
            int col = matcher.start(1) - 1;
            if (aIndex == 0) {
                col += aStartCol;
            }
            if (matcher.groupCount() > 1) {
                aTags.add(new JavadocTag(aCurrentLine, col,
                                         matcher.group(1), matcher.group(2)));
            } else {
                aTags.add(new JavadocTag(aCurrentLine, col, matcher.group(1)));
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * If we have a match for a multiline parameter tag, add to aTags and
     * return true.
     *
     * @param aTags list of tags to modify.
     * @param aLines lines of comment.
     * @param aIndex index of current line in aLines.
     * @param aTagPattern pattern for tag.
     * @param aCurrentLine current source line number.
     * @param aStartCol starting column of first line in aLines.
     * @return True iff match for aTagPattern found.
     */
    private boolean addMultilineMethodTag(final List<JavadocTag> aTags,
                                          final String[] aLines,
                                          int aIndex,
                                          final Pattern aTagPattern,
                                          int aCurrentLine,
                                          int aStartCol) {
        Matcher matcher = aTagPattern.matcher(aLines[aIndex]);
        if (matcher.find()) {
            int col = matcher.start(1) - 1;
            if (aIndex == 0) {
                col += aStartCol;
            }

            // Look for the rest of the comment if all we saw was
            // the tag and the name. Stop when we see '*/' (end of
            // Javadoc), '@' (start of next tag), or anything that's
            // not whitespace or '*' characters.
            int remIndex = aIndex + 1;
            while (remIndex < aLines.length) {
                final Matcher multilineCont = MATCH_JAVADOC_MULTILINE_CONT
                    .matcher(aLines[remIndex]);
                if (multilineCont.find()) {
                    final String lFin = multilineCont.group(1);
                    if (!lFin.equals(NEXT_TAG)
                        && !lFin.equals(END_JAVADOC)) {
                        if (matcher.groupCount() > 1) {
                            aTags.add(new JavadocTag(aCurrentLine, col,
                                                     matcher.group(1),
                                                     matcher.group(2)));
                        } else {
                            aTags.add(new JavadocTag(aCurrentLine, col,
                                                     matcher.group(1)));
                        }
                    }
                    break;
                }
                remIndex++;
            }

            return true;
        }
        return false;
    }

    /**
     * Returns the tags in a javadoc comment. Only finds throws, exception,
     * param, return and see tags.
     *
     * @return the tags found
     * @param aLines the Javadoc comment text as an array of lines.
     * @param aStartLine the line number of the first line of lines.
     * @param aStartCol the column number of the first character of
     *          each comment line.
     */
    private List<JavadocTag> getMethodTags(final String[] aLines,
                                           int aStartLine,
                                           int aStartCol) {
        final List<JavadocTag> tags = Lists.newArrayList();
        int currentLine = aStartLine - 1;

        final Pattern[] simplePatterns = {
            MATCH_JAVADOC_ARG,
            MATCH_JAVADOC_NOARG,
            MATCH_JAVADOC_NOARG_CURLY
        };
        final Pattern[] multilinePatterns = {
            MATCH_JAVADOC_ARG_MULTILINE_START,
            MATCH_JAVADOC_NOARG_MULTILINE_START
        };


    findTags:
        for (int i = 0; i < aLines.length; i++) {
            currentLine++;

            for (Pattern patn : simplePatterns) {
                if (addSimpleMethodTag(tags, aLines, i, patn,
                                       currentLine, aStartCol)) {
                    continue findTags;
                }
            }

            for (Pattern patn : multilinePatterns) {
                if (addMultilineMethodTag(tags, aLines, i, patn,
                                          currentLine, aStartCol)) {

                    continue findTags;
                }
            }
        }

        return tags;
    }

    /**
     * Computes the parameter nodes for a method.
     *
     * @param ast the method node.
     * @return the list of parameter nodes for ast.
     */
    private List<DetailAST> getParameters(DetailAST ast) {
        final DetailAST params = ast.findFirstToken(TokenTypes.PARAMETERS);
        final List<DetailAST> retVal = Lists.newArrayList();

        DetailAST child = params.getFirstChild();
        while (child != null) {
            if (child.getType() == TokenTypes.PARAMETER_DEF) {
                final DetailAST ident = child.findFirstToken(TokenTypes.IDENT);
                retVal.add(ident);
            }
            child = child.getNextSibling();
        }
        return retVal;
    }

    /**
     * Computes the exception nodes for a method.
     *
     * @param ast the method node.
     * @return the list of exception nodes for ast.
     */
    private List<ExceptionInfo> getThrows(DetailAST ast) {
        final List<ExceptionInfo> retVal = Lists.newArrayList();
        final DetailAST throwsAST = ast
                .findFirstToken(TokenTypes.LITERAL_THROWS);
        if (throwsAST != null) {
            DetailAST child = throwsAST.getFirstChild();
            while (child != null) {
                if (child.getType() == TokenTypes.IDENT
                        || child.getType() == TokenTypes.DOT) {
                    final FullIdent fi = FullIdent.createFullIdent(child);
                    final ExceptionInfo ei = new ExceptionInfo(createClassInfo(new Token(fi),
                            getCurrentClassName()));
                    retVal.add(ei);
                }
                child = child.getNextSibling();
            }
        }
        return retVal;
    }

    /**
     * Remove and return parameter tag matching aText from aTags.
     *
     * @return tag found, or null if none matches.
     * @param aText tag name to search for
     * @param aTags set of tags to search
     */
    private JavadocTag findRemoveParamTag(String aText,
                                          final List<JavadocTag> aTags) {
        final ListIterator<JavadocTag> tagIt = aTags.listIterator();
        while (tagIt.hasNext()) {
            final JavadocTag tag = tagIt.next();

            if (!tag.isParamTag()) {
                continue;
            }

            if (aText.equals(tag.getFirstArg())) {
                tagIt.remove();
                return tag;
            }
        }
        return null;
    }

    /**
     * Returns true iff aName appears as a complete, upper-cased word
     * in commentText.
     *
     * @param aName name to search for.
     * @param commentText lines of text to search.
     * @return whether aName appears in commentText.
     */
    private boolean findMention(String aName, final String[] commentText) {
        Pattern searchPatn =
            Pattern.compile("\\b" + aName.toUpperCase() + "\\b");
        for (String line : commentText) {
            if (searchPatn.matcher(line).find()) {
                return true;
            }
        }
        return false;
    }

    /** Pattern describing a possible word describing return of a value. */
    private static final Pattern RETURN_NARRATIVE_PATN =
        Pattern.compile("\\b(return|yield)(s|ing)?\\b",
                        Pattern.CASE_INSENSITIVE);

    /**
     * Returns true iff a word suggestive of returning appears in
     * commentText.
     *
     * @param commentText lines of text to search.
     * @return whether a narrative return description appears in commentText.
     */
    private boolean findReturnNarrative(final String[] commentText) {
        Matcher matcher = RETURN_NARRATIVE_PATN.matcher("");
        for (String line : commentText) {
            matcher.reset(line);
            if (matcher.find())
                return true;
        }
        return false;
    }

    /**
     * Checks that parameters and return value are properly commented on.
     * Removes parameter tags from aTags as a side effect.
     *
     * @param aTags the tags to check
     * @param aParent the node which takes the parameters
     * @param commentText the text of the comment, as an array of lines.
     * @param aReportExpectedTags whether we should report if do not find
     *            expected tag
     */
    private void checkParams(final List<JavadocTag> aTags,
                             final DetailAST aParent,
                             final String[] commentText,
                             boolean aReportExpectedTags) {
        boolean tagFound = false;
        boolean narrativeFound = false;

        for (DetailAST param : getParameters(aParent)) {
            JavadocTag tag = findRemoveParamTag(param.getText(), aTags);

            if (unusedParamFormatRE != null
                && unusedParamFormatRE.matcher(param.getText()).matches()) {
                continue;
            }

            if (tag != null) {
                tagFound = true;
            } else if (allowNarrativeParamTags
                       && findMention(param.getText(), commentText)) {
                narrativeFound = true;
            } else if (!allowMissingParamTags && aReportExpectedTags) {
                log(param, "javadoc.expectedTag",
                    JavadocTagInfo.PARAM.getText(), param.getText());
            }
        }

        for (DetailAST typeParam : CheckUtils.getTypeParameters(aParent)) {
            String tagName =
                typeParam.findFirstToken(TokenTypes.IDENT).getText();
            String tagText = "<" + tagName + ">";
            JavadocTag tag = findRemoveParamTag(tagText, aTags);

            if (unusedParamFormatRE != null
                && unusedParamFormatRE.matcher(tagName).matches()) {
                continue;
            }

            if (tag != null) {
                tagFound = true;
            } else if (allowNarrativeParamTags
                       && findMention(tagName, commentText)) {
                narrativeFound = true;
            } else if (!allowMissingParamTags && aReportExpectedTags) {
                log(typeParam, "javadoc.expectedTag",
                    JavadocTagInfo.PARAM.getText(), tagText);
            }
        }

        if (tagFound && narrativeFound) {
            log(aParent, "javadoc.mixedStyle");
        }

        final ListIterator<JavadocTag> tagIt = aTags.listIterator();
        while (tagIt.hasNext()) {
            JavadocTag tag = tagIt.next();
            if (tag.isParamTag()) {
                log(tag.getLineNo(), tag.getColumnNo(), "javadoc.unusedTag",
                    "@param", tag.getFirstArg());
                tagIt.remove();
            }
        }
    }

    /**
     * Checks whether a method is a function.
     *
     * @param ast the method node.
     * @return whether the method is a function.
     */
    private boolean isFunction(DetailAST ast) {
        boolean retVal = false;
        if (ast.getType() == TokenTypes.METHOD_DEF) {
            final DetailAST typeAST = ast.findFirstToken(TokenTypes.TYPE);
            if (typeAST != null
                && typeAST.findFirstToken(TokenTypes.LITERAL_VOID) == null) {
                retVal = true;
            }
        }
        return retVal;
    }

    /**
     * Checks for only one return tag. All return tags will be removed from the
     * supplied list.
     *
     * @param tags the tags to check
     * @param lineNo the line number of the expected tag
     * @param reportExpectedTags whether we should report if do not find
     *            expected tag
     */
    private void checkReturnTag(List<JavadocTag> tags, int lineNo,
                                final String[] commentText,
                                boolean reportExpectedTags) {
        // Loop over tags finding return tags. After the first one, report an
        // error.
        boolean found = false;
        final ListIterator<JavadocTag> it = tags.listIterator();
        while (it.hasNext()) {
            final JavadocTag jt = it.next();
            if (jt.isReturnTag()) {
                if (found) {
                    log(jt.getLineNo(), jt.getColumnNo(),
                        MSG_DUPLICATE_TAG,
                        JavadocTagInfo.RETURN.getText());
                }
                found = true;
                it.remove();
            }
        }

        // Handle there being no @return tags :- unless
        // the user has chosen to suppress these problems
        if (!found && !allowMissingReturnTag && reportExpectedTags
            && (!allowNarrativeReturnTags
                || !findReturnNarrative(commentText))) {
            log(lineNo, MSG_RETURN_EXPECTED);
        }
    }

     /**
     * Return true and mark exception info iff exception named aName occurs
     * in aThrows.
     *
     * @param aThrows list of exceptions to check for aName.
     * @param aName name to search for.
     * @return true iff aName found in aThrows.
     */
    private boolean exceptionMatchedByName(final List<ExceptionInfo> aThrows,
                                           String aName) {
        for (ExceptionInfo ei : aThrows) {
            if (ei.getName().getText().equals(aName)) {
                ei.setFound();
                return true;
            }
        }
        return false;
    }

    /**
     * Return true and mark exception info iff exception of class aClazz
     * occurs in aThrows.
     *
     * @param aThrows list of exceptions to check for aClazz.
     * @param aClazz name to search for.
     * @return true iff aClazz found in aThrows.
     */
    private boolean exceptionMatchedByClass(final List<ExceptionInfo> aThrows,
                                            final Class<?> aClazz) {
        for (ExceptionInfo ei : aThrows) {
            if (aClazz == ei.getClazz()) {
                ei.setFound();
                return true;
            } else if (allowThrowsTagsForSubclasses
                       && isSubclass(aClazz, ei.getClazz())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks a set of tags for matching throws.
     *
     * @param tags the tags to check
     * @param aThrows the throws to check
     * @param reportExpectedTags whether we should report if do not find
     *            expected tag
     */
    private void checkThrowsTags(List<JavadocTag> tags,
            List<ExceptionInfo> aThrows, boolean reportExpectedTags) {
        // Loop over the tags, checking to see they exist in the throws.
        final ListIterator<JavadocTag> tagIt = tags.listIterator();
        while (tagIt.hasNext()) {
            final JavadocTag tag = tagIt.next();

            if (!tag.isThrowsTag()) {
                continue;
            }

            tagIt.remove();

            // Loop looking for matching throw
            final String documentedEx = tag.getFirstArg();
            final Token token = new Token(tag.getFirstArg(), tag.getLineNo(), tag
                    .getColumnNo());
            final AbstractClassInfo documentedCI = createClassInfo(token,
                    getCurrentClassName());

            if (!exceptionMatchedByName(aThrows,
                                        documentedCI.getName().getText())
                && !exceptionMatchedByClass(aThrows,
                                            documentedCI.getClazz())) {
                // Handle extra JavadocTag.
                boolean reqd = true;
                if (allowUndeclaredRTE) {
                    reqd = !isUnchecked(documentedCI.getClazz());
                }

                if (reqd) {
                    log(tag.getLineNo(), tag.getColumnNo(),
                        "javadoc.unusedTag",
                        JavadocTagInfo.THROWS.getText(), tag.getFirstArg());
                }
            }
        }

        if (!allowMissingThrowsTags && reportExpectedTags) {
            for (ExceptionInfo ei : aThrows) {
                if (!ei.isFound()) {
                    final Token fi = ei.getName();
                    log(fi.getLineNo(), fi.getColumnNo(),
                        "javadoc.expectedTag",
                        JavadocTagInfo.THROWS.getText(), fi.getText());
                }
            }
        }
    }

    /**
     * Returns whether an AST represents a setter method.
     * @param ast the AST to check with
     * @return whether the AST represents a setter method
     */
    private boolean isSetterMethod(final DetailAST ast) {
        // Check have a method with exactly 7 children which are all that
        // is allowed in a proper setter method which does not throw any
        // exceptions.
        if (ast.getType() != TokenTypes.METHOD_DEF
                || ast.getChildCount() != MAX_CHILDREN) {
            return false;
        }

        // Should I handle only being in a class????

        // Check the name matches format setX...
        final DetailAST type = ast.findFirstToken(TokenTypes.TYPE);
        final String name = type.getNextSibling().getText();
        if (!name.matches("^set[A-Z].*")) { // Depends on JDK 1.4
            return false;
        }

        // Check the return type is void
        if (type.getChildCount(TokenTypes.LITERAL_VOID) == 0) {
            return false;
        }

        // Check that is had only one parameter
        final DetailAST params = ast.findFirstToken(TokenTypes.PARAMETERS);
        if (params == null
                || params.getChildCount(TokenTypes.PARAMETER_DEF) != 1) {
            return false;
        }

        // Now verify that the body consists of:
        // SLIST -> EXPR -> ASSIGN
        // SEMI
        // RCURLY
        final DetailAST slist = ast.findFirstToken(TokenTypes.SLIST);
        if (slist == null || slist.getChildCount() != BODY_SIZE) {
            return false;
        }

        final AST expr = slist.getFirstChild();
        if (expr.getType() != TokenTypes.EXPR
                || expr.getFirstChild().getType() != TokenTypes.ASSIGN) {
            return false;
        }

        return true;
    }

    /**
     * Returns whether an AST represents a getter method.
     * @param ast the AST to check with
     * @return whether the AST represents a getter method
     */
    private boolean isGetterMethod(final DetailAST ast) {
        // Check have a method with exactly 7 children which are all that
        // is allowed in a proper getter method which does not throw any
        // exceptions.
        if (ast.getType() != TokenTypes.METHOD_DEF
                || ast.getChildCount() != MAX_CHILDREN) {
            return false;
        }

        // Check the name matches format of getX or isX. Technically I should
        // check that the format isX is only used with a boolean type.
        final DetailAST type = ast.findFirstToken(TokenTypes.TYPE);
        final String name = type.getNextSibling().getText();
        if (!name.matches("^(is|get)[A-Z].*")) { // Depends on JDK 1.4
            return false;
        }

        // Check the return type is void
        if (type.getChildCount(TokenTypes.LITERAL_VOID) > 0) {
            return false;
        }

        // Check that is had only one parameter
        final DetailAST params = ast.findFirstToken(TokenTypes.PARAMETERS);
        if (params == null
                || params.getChildCount(TokenTypes.PARAMETER_DEF) > 0) {
            return false;
        }

        // Now verify that the body consists of:
        // SLIST -> RETURN
        // RCURLY
        final DetailAST slist = ast.findFirstToken(TokenTypes.SLIST);
        if (slist == null || slist.getChildCount() != 2) {
            return false;
        }

        final AST expr = slist.getFirstChild();
        if (expr.getType() != TokenTypes.LITERAL_RETURN
                || expr.getFirstChild().getType() != TokenTypes.EXPR) {
            return false;
        }

        return true;
    }

    /** Stores useful information about declared exception. */
    private static class ExceptionInfo {
        /** does the exception have throws tag associated with. */
        private boolean found;
        /** class information associated with this exception. */
        private final AbstractClassInfo classInfo;

        /**
         * Creates new instance for <code>FullIdent</code>.
         *
         * @param classInfo clas info
         */
        ExceptionInfo(AbstractClassInfo classInfo) {
            this.classInfo = classInfo;
        }

        /** Mark that the exception has associated throws tag */
        final void setFound() {
            found = true;
        }

        /** @return whether the exception has throws tag associated with */
        final boolean isFound() {
            return found;
        }

        /** @return exception's name */
        final Token getName() {
            return classInfo.getName();
        }

        /** @return class for this exception */
        final Class<?> getClazz() {
            return classInfo.getClazz();
        }
    }
}
