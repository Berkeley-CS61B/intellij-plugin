////////////////////////////////////////////////////////////////////////////////
// checkstyle: Checks Java source code for adherence to a set of rules.
// Copyright (C) 2001-2012  Oliver Burn
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

import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.FileContents;
import com.puppycrawl.tools.checkstyle.api.Scope;
import com.puppycrawl.tools.checkstyle.api.TextBlock;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import com.puppycrawl.tools.checkstyle.checks.javadoc.InvalidJavadocTag;
import com.puppycrawl.tools.checkstyle.checks.javadoc.JavadocTag;
import com.puppycrawl.tools.checkstyle.checks.javadoc.JavadocTagInfo;
import com.puppycrawl.tools.checkstyle.checks.javadoc.JavadocTags;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.puppycrawl.tools.checkstyle.utils.CheckUtil;
import com.puppycrawl.tools.checkstyle.utils.JavadocUtil;
import com.puppycrawl.tools.checkstyle.utils.ScopeUtil;
import org.apache.commons.beanutils.ConversionException;

/**
 * Checks the Javadoc of a type.
 *
 * @author Oliver Burn
 * @author Michael Tamm
 * @author P. N. Hilfinger (modifications)
 */
public class JavadocType61bCheck
        extends AbstractCheck
{
    /** the scope to check for */
    private Scope mScope = Scope.PRIVATE;
    /** the visibility scope where Javadoc comments shouldn't be checked **/
    private Scope mExcludeScope;
    /** compiled regexp to match author tag content **/
    private Pattern mAuthorFormatPattern;
    /** compiled regexp to match version tag content **/
    private Pattern mVersionFormatPattern;
    /** regexp to match author tag content */
    private String mAuthorFormat;
    /** regexp to match version tag content */
    private String mVersionFormat;
    /**
     * controls whether to ignore errors when a method has type parameters but
     * does not have matching param tags in the javadoc. Defaults to false.
     */
    private boolean mAllowMissingParamTags;
    /** controls whether to flag errors for unknown tags. Defaults to false. */
    private boolean mAllowUnknownTags;

    /**
     * Sets the scope to check.
     * @param aFrom string to set scope from
     */
    public void setScope(String aFrom)
    {
        mScope = Scope.getInstance(aFrom);
    }

    /**
     * Set the excludeScope.
     * @param aScope a <code>String</code> value
     */
    public void setExcludeScope(String aScope)
    {
        mExcludeScope = Scope.getInstance(aScope);
    }

    /**
     * Set the author tag pattern.
     * @param aFormat a <code>String</code> value
     * @throws ConversionException unable to parse aFormat
     */
    public void setAuthorFormat(String aFormat)
            throws ConversionException
    {
        try {
            mAuthorFormat = aFormat;
            mAuthorFormatPattern = Pattern.compile(aFormat);
        }
        catch (final PatternSyntaxException e) {
            throw new ConversionException("unable to parse " + aFormat, e);
        }
    }

    /**
     * Set the version format pattern.
     * @param aFormat a <code>String</code> value
     * @throws ConversionException unable to parse aFormat
     */
    public void setVersionFormat(String aFormat)
            throws ConversionException
    {
        try {
            mVersionFormat = aFormat;
            mVersionFormatPattern = Pattern.compile(aFormat);
        }
        catch (final PatternSyntaxException e) {
            throw new ConversionException("unable to parse " + aFormat, e);
        }

    }

    /**
     * Controls whether to allow a type which has type parameters to
     * omit matching param tags in the javadoc. Defaults to false.
     *
     * @param aFlag a <code>Boolean</code> value
     */
    public void setAllowMissingParamTags(boolean aFlag)
    {
        mAllowMissingParamTags = aFlag;
    }

    /**
     * Controls whether to flag errors for unknown tags. Defaults to false.
     * @param aFlag a <code>Boolean</code> value
     */
    public void setAllowUnknownTags(boolean aFlag)
    {
        mAllowUnknownTags = aFlag;
    }

    @Override
    public int[] getDefaultTokens()
    {
        return getRequiredTokens();
    }

    @Override
    public int[] getAcceptableTokens()
    {
        return getRequiredTokens();
    }

    @Override
    public int[] getRequiredTokens()
    {
        return new int[] {
                TokenTypes.INTERFACE_DEF,
                TokenTypes.CLASS_DEF,
                TokenTypes.ENUM_DEF,
                TokenTypes.ANNOTATION_DEF,
        };
    }

    @Override
    public void visitToken(DetailAST aAST)
    {
        if (shouldCheck(aAST)) {
            final FileContents contents = getFileContents();
            final int lineNo = aAST.getLineNo();
            final TextBlock cmt = contents.getJavadocBefore(lineNo);
            if (cmt == null) {
                log(lineNo, "javadoc.missing");
            }
            else if (ScopeUtil.isOuterMostType(aAST)) {
                // don't check author/version for inner classes
                final List<JavadocTag> tags = getJavadocTags(cmt);
                checkTag(lineNo, tags, JavadocTagInfo.AUTHOR.getName(),
                        mAuthorFormatPattern, mAuthorFormat);
                checkTag(lineNo, tags, JavadocTagInfo.VERSION.getName(),
                        mVersionFormatPattern, mVersionFormat);

                final List<String> typeParamNames =
                        CheckUtil.getTypeParameterNames(aAST);

                if (!mAllowMissingParamTags) {
                    //Check type parameters that should exist, do
                    for (final String string : typeParamNames) {
                        checkTypeParamTag(
                                lineNo, tags, string);
                    }
                }

                checkUnusedTypeParamTags(tags, typeParamNames);
            }
        }
    }

    /**
     * Whether we should check this node.
     * @param aAST a given node.
     * @return whether we should check a given node.
     */
    private boolean shouldCheck(final DetailAST aAST)
    {
        if (ScopeUtil.isInCodeBlock(aAST)) {
            return false;
        }

        final DetailAST mods = aAST.findFirstToken(TokenTypes.MODIFIERS);
        final Scope declaredScope = ScopeUtil.getScopeFromMods(mods);
        final Scope scope =
                ScopeUtil.isInInterfaceOrAnnotationBlock(aAST)
                        ? Scope.PUBLIC : declaredScope;
        final Scope surroundingScope = ScopeUtil.getSurroundingScope(aAST);

        return scope.isIn(mScope)
                && ((surroundingScope == null) || surroundingScope.isIn(mScope))
                && ((mExcludeScope == null)
                || !scope.isIn(mExcludeScope)
                || ((surroundingScope != null)
                && !surroundingScope.isIn(mExcludeScope)));
    }

    /**
     * Gets all standalone tags from a given javadoc.
     * @param aCmt the Javadoc comment to process.
     * @return all standalone tags from the given javadoc.
     */
    private List<JavadocTag> getJavadocTags(TextBlock aCmt)
    {
        final JavadocTags tags = JavadocUtil.getJavadocTags(aCmt,
                JavadocUtil.JavadocTagType.BLOCK);
        if (!mAllowUnknownTags) {
            for (final InvalidJavadocTag tag : tags.getInvalidTags()) {
                log(tag.getLine(), tag.getCol(), "javadoc.unknownTag",
                        tag.getName());
            }
        }
        return tags.getValidTags();
    }

    /**
     * Verifies that a type definition has a required tag.
     * @param aLineNo the line number for the type definition.
     * @param aTags tags from the Javadoc comment for the type definition.
     * @param aTag the required tag name.
     * @param aFormatPattern regexp for the tag value.
     * @param aFormat pattern for the tag value.
     */
    private void checkTag(int aLineNo, List<JavadocTag> aTags, String aTag,
                          Pattern aFormatPattern, String aFormat)
    {
        if (aFormatPattern == null) {
            return;
        }

        int tagCount = 0;
        for (int i = aTags.size() - 1; i >= 0; i--) {
            final JavadocTag tag = aTags.get(i);
            if (tag.getTagName().equals(aTag)) {
                tagCount++;
                if (!aFormatPattern.matcher(tag.getFirstArg()).find()) {
                    log(aLineNo, "type.tagFormat", "@" + aTag, aFormat);
                }
            }
        }
        if (tagCount == 0) {
            log(aLineNo, "type.missingTag", "@" + aTag);
        }
    }

    /**
     * Verifies that a type definition has the specified param tag for
     * the specified type parameter name.
     * @param aLineNo the line number for the type definition.
     * @param aTags tags from the Javadoc comment for the type definition.
     * @param aTypeParamName the name of the type parameter
     */
    private void checkTypeParamTag(final int aLineNo,
                                   final List<JavadocTag> aTags, final String aTypeParamName)
    {
        boolean found = false;
        for (int i = aTags.size() - 1; i >= 0; i--) {
            final JavadocTag tag = aTags.get(i);
            if (tag.isParamTag()
                    && (tag.getFirstArg() != null)
                    && (tag.getFirstArg().indexOf("<" + aTypeParamName + ">") == 0))
            {
                found = true;
            }
        }
        if (!found) {
            log(aLineNo, "type.missingTag",
                    JavadocTagInfo.PARAM.getText() + " <" + aTypeParamName + ">");
        }
    }

    /**
     * Checks for unused param tags for type parameters.
     * @param aTags tags from the Javadoc comment for the type definition.
     * @param aTypeParamNames names of type parameters
     */
    private void checkUnusedTypeParamTags(
            final List<JavadocTag> aTags,
            final List<String> aTypeParamNames)
    {
        final Pattern pattern = Pattern.compile("\\s*<([^>]+)>.*");
        for (int i = aTags.size() - 1; i >= 0; i--) {
            final JavadocTag tag = aTags.get(i);
            if (tag.isParamTag()) {

                if (tag.getFirstArg() != null) {

                    final Matcher matcher = pattern.matcher(tag.getFirstArg());
                    String typeParamName = null;

                    if (matcher.matches()) {
                        typeParamName = matcher.group(1).trim();
                        if (!aTypeParamNames.contains(typeParamName)) {
                            log(tag.getLineNo(), tag.getColumnNo(),
                                    "javadoc.unusedTag",
                                    JavadocTagInfo.PARAM.getText(),
                                    "<" + typeParamName + ">");
                        }
                    }
                    else {
                        log(tag.getLineNo(), tag.getColumnNo(),
                                "javadoc.unusedTagGeneral");
                    }
                }
                else {
                    log(tag.getLineNo(), tag.getColumnNo(),
                            "javadoc.unusedTagGeneral");
                }
            }
        }
    }
}
