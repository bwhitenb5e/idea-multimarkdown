/*
 * Copyright (c) 2015-2015 Vladimir Schneider <vladimir.schneider@gmail.com>
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.vladsch.idea.multimarkdown.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.vladsch.idea.multimarkdown.psi.MultiMarkdownFile;
import com.vladsch.idea.multimarkdown.psi.MultiMarkdownWikiPageRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FileReferenceListQuery {
    // types of files to search
    public final static int ANY_FILE = 0x0000;
    public final static int IMAGE_FILE = 0x0001;
    public final static int WIKIPAGE_FILE = 0x0002;
    public final static int MARKDOWN_FILE = 0x0003;

    public final static int FILE_TYPE_FLAGS = 0x000f;

    public final static int INCLUDE_SOURCE = 0x0010;

    // comparison options
    public final static int SPACE_DASH_EQUIVALENT = 0x0020;
    public final static int CASE_INSENSITIVE = 0x0040;

    // what is provided for the match
    public final static int LINK_WITH_EXT_REF = 0x0000;
    public final static int WIKIPAGE_REF = 0x0080;
    public final static int LINK_REF_NO_EXT = 0x0100;

    public final static int MATCH_TYPE_FLAGS = LINK_REF_NO_EXT | WIKIPAGE_REF;

    protected final @NotNull FileReferenceList defaultFileList;
    protected int queryFlags;
    protected String matchLinkRef;
    protected FileReference sourceReference;

    public FileReferenceListQuery(@NotNull FileReferenceList defaultFileList) {
        this.defaultFileList = defaultFileList;
        this.queryFlags = 0;
        this.matchLinkRef = null;
        this.sourceReference = null;
    }

    public FileReferenceListQuery(@NotNull FileReferenceList defaultFileList, int queryFlags) {
        this.defaultFileList = defaultFileList;
        this.queryFlags = queryFlags;
        this.matchLinkRef = null;
        this.sourceReference = null;
    }

    public FileReferenceListQuery(@NotNull FileReferenceListQuery other) {
        this.defaultFileList = other.defaultFileList;
        this.queryFlags = other.queryFlags;
        this.matchLinkRef = other.matchLinkRef;
        this.sourceReference = other.sourceReference;
    }

    @NotNull
    public FileReferenceList detDefaultFileList() {
        return defaultFileList;
    }

    public int getQueryFlags() {
        return queryFlags;
    }

    public String getMatchLinkRef() {
        return matchLinkRef;
    }

    public FileReference getSourceReference() {
        return sourceReference;
    }

    @NotNull
    public FileReferenceListQuery wikiPages() {
        queryFlags = (queryFlags & ~FILE_TYPE_FLAGS) | WIKIPAGE_FILE;
        return this;
    }

    @NotNull
    public FileReferenceListQuery markdownFiles() {
        queryFlags = (queryFlags & ~FILE_TYPE_FLAGS) | MARKDOWN_FILE;
        return this;
    }

    @NotNull
    public FileReferenceListQuery imageFiles() {
        queryFlags = (queryFlags & ~FILE_TYPE_FLAGS) | IMAGE_FILE;
        return this;
    }

    @NotNull
    public FileReferenceListQuery allFiles() {
        queryFlags = (queryFlags & ~FILE_TYPE_FLAGS);
        return this;
    }

    @NotNull
    public FileReferenceListQuery spaceDashEqual() {
        queryFlags |= SPACE_DASH_EQUIVALENT;
        return this;
    }

    @NotNull
    public FileReferenceListQuery spaceDashNotEqual() {
        queryFlags &= ~SPACE_DASH_EQUIVALENT;
        return this;
    }

    @NotNull
    public FileReferenceListQuery caseInsensitive() {
        queryFlags |= CASE_INSENSITIVE;
        return this;
    }

    @NotNull
    public FileReferenceListQuery caseSensitive() {
        queryFlags &= ~CASE_INSENSITIVE;
        return this;
    }

    @NotNull
    public FileReferenceListQuery matchAnyRef() {
        this.matchLinkRef = null;
        queryFlags &= ~MATCH_TYPE_FLAGS;
        return this;
    }

    @NotNull
    public FileReferenceListQuery matchWikiRef(@Nullable String wikiRef) {
        // set wiki page files as default if not markdown or wikipages are already set
        if ((this.queryFlags & FILE_TYPE_FLAGS) == 0) this.wikiPages();
        this.matchLinkRef = wikiRef;
        queryFlags = (queryFlags & ~MATCH_TYPE_FLAGS) | WIKIPAGE_REF;
        return this;
    }

    @NotNull
    public FileReferenceListQuery matchWikiRef(@NotNull MultiMarkdownWikiPageRef wikiPageRef) {
        return inSource(new FileReference(wikiPageRef.getContainingFile()))
                .matchWikiRef(wikiPageRef.getName());
    }

    @NotNull
    public FileReferenceListQuery matchLinkRef(@NotNull String linkRef, boolean withExt) {
        this.matchLinkRef = linkRef;
        queryFlags = (queryFlags & ~MATCH_TYPE_FLAGS) | (withExt ? LINK_WITH_EXT_REF : LINK_REF_NO_EXT);
        return this;
    }

    @NotNull
    public FileReferenceListQuery matchLinkRefNoExt(@NotNull String linkRef) {
        return matchLinkRef(linkRef, false);
    }

    @NotNull
    public FileReferenceListQuery matchLinkRefNoExt(@NotNull String href, @NotNull VirtualFile virtualFile, @NotNull Project project) {
        return inSource(virtualFile, project)
                .matchLinkRef(href, false);
    }

    @NotNull
    public FileReferenceListQuery matchLinkRefWithExt(@NotNull String href, @NotNull VirtualFile virtualFile, @NotNull Project project) {
        return inSource(virtualFile, project)
                .matchLinkRef(href, false);
    }

    @NotNull
    public FileReferenceListQuery matchLinkRef(@NotNull String href, @NotNull VirtualFile virtualFile, @NotNull Project project) {
        return inSource(virtualFile, project)
                .matchLinkRef(href, new FilePathInfo(href).hasExt());
    }

    @NotNull
    public FileReferenceListQuery matchLinkRef(@NotNull String linkRef) {
        return matchLinkRef(linkRef, true);
    }

    @NotNull
    public FileReferenceListQuery inSource(@NotNull FileReference sourceFileReference) {
        // set default file types to wikipage if source is a wikipage
        if ((queryFlags & FILE_TYPE_FLAGS) == 0) queryFlags |= (sourceFileReference.isWikiPage()) ? WIKIPAGE_FILE : MARKDOWN_FILE;
        this.sourceReference = sourceFileReference;
        return this;
    }

    @NotNull
    public FileReferenceListQuery inSource(@NotNull PsiFile sourceMarkdownFile) {
        return inSource(new FileReference(sourceMarkdownFile.getVirtualFile(), sourceMarkdownFile.getProject()));
    }

    @NotNull
    public FileReferenceListQuery inSource(@NotNull VirtualFile virtualFile, @NotNull Project project) {
        return inSource(new FileReference(virtualFile, project));
    }

    @NotNull
    public FileReferenceListQuery inAnySource() {
        this.sourceReference = null;
        return this;
    }

    @NotNull
    public FileReferenceListQuery includeSource() {
        this.queryFlags |= INCLUDE_SOURCE;
        return this;
    }

    @NotNull
    protected FileReferenceList buildResults(@Nullable FileReferenceList fileList, FileReferenceList.Filter... postFilters) {
        if (fileList == null) fileList = defaultFileList;

        int iMax = postFilters.length;
        FileReferenceList.Filter[] filters = new FileReferenceList.Filter[iMax + 2];
        filters[0] = getFileTypeFilter(queryFlags);
        filters[1] = getQueryFilter();
        if (iMax > 0) System.arraycopy(postFilters, 0, filters, 2, iMax);
        return new FileReferenceList(filters, fileList);
    }

    protected static FileReferenceList.Filter getFileTypeFilter(int searchFlags) {
        FileReferenceList.Filter filter;

        switch (searchFlags & FILE_TYPE_FLAGS) {
            case IMAGE_FILE:
                filter = FileReferenceList.IMAGE_FILE_FILTER;
                break;

            case MARKDOWN_FILE:
                filter = FileReferenceList.MARKDOWN_FILE_FILTER;
                break;

            case WIKIPAGE_FILE:
                filter = FileReferenceList.WIKIPAGE_FILE_FILTER;
                break;

            default:
            case ANY_FILE:
                filter = FileReferenceList.ANY_FILE_FILTER;
                break;
        }
        return filter;
    }

    @Nullable
    public FileReferenceList.Filter getQueryFilter() {
        return getQueryFilter(sourceReference, matchLinkRef, queryFlags);
    }

    @NotNull
    public FileReferenceList getList() {
        return buildResults(null);
    }

    @NotNull
    public FileReferenceList getList(@NotNull FileReferenceList fileReferenceList) {
        return buildResults(fileReferenceList);
    }

    @NotNull
    public FileReferenceList getList(FileReferenceList.Filter... queryFilters) {
        return buildResults(null, queryFilters);
    }

    @NotNull
    public FileReferenceList getAccessibleWikiPageRefs() {
        return buildResults(null, FileReferenceList.ACCESSIBLE_WIKI_REFS_FILTER);
    }

    @NotNull
    public FileReferenceList getInaccessibleWikiPageRefs() {
        return buildResults(null, FileReferenceList.INACCESSIBLE_WIKI_REFS_FILTER);
    }

    @NotNull
    public FileReferenceList getAllWikiPageRefs() {
        return buildResults(null, FileReferenceList.ALL_WIKI_REFS_FILTER);
    }

    @NotNull
    public FileReferenceList getWikiPageRefs(boolean allowInaccessibleRefs) {
        return buildResults(null,
                allowInaccessibleRefs ? FileReferenceList.ALL_WIKI_REFS_FILTER : FileReferenceList.ACCESSIBLE_WIKI_REFS_FILTER);
    }

    @NotNull
    public FileReferenceList getList(@NotNull FileReferenceList fileReferenceList, FileReferenceList.Filter... queryFilters) {
        return buildResults(fileReferenceList, queryFilters);
    }

    @NotNull
    public VirtualFile[] getVirtualFiles() {
        return buildResults(null).getVirtualFiles();
    }

    @NotNull
    public PsiFile[] getPsiFiles() {
        return buildResults(null).getPsiFiles();
    }

    @NotNull
    public MultiMarkdownFile[] getMarkdownFiles() {
        return buildResults(null).getMarkdownFiles();
    }

    @NotNull
    public MultiMarkdownFile[] getWikiPageFiles(boolean allowInaccessiblePages) {
        return allowInaccessiblePages ? getAllWikiPageFiles() : getAccessibleWikiPageFiles();
    }

    @NotNull
    public MultiMarkdownFile[] getAllWikiPageFiles() {
        return buildResults(null).getAllWikiPageFiles();
    }

    @NotNull
    public MultiMarkdownFile[] getAccessibleWikiPageFiles() {
        return buildResults(null).getAccessibleWikiPageFiles();
    }

    @NotNull
    public MultiMarkdownFile[] getInaccessibleWikiPageFiles() {
        return buildResults(null).getInaccessibleWikiPageFiles();
    }

    // Implementation details for queries and lists
    public static boolean endsWith(int searchFlags, @NotNull String fileRef, @NotNull String wikiRef) {
        return FilePathInfo.endsWith((searchFlags & CASE_INSENSITIVE) == 0, (searchFlags & SPACE_DASH_EQUIVALENT) != 0, fileRef, wikiRef);
    }

    public static boolean equivalent(int searchFlags, @NotNull String fileRef, @NotNull String wikiRef) {
        return FilePathInfo.equivalent((searchFlags & CASE_INSENSITIVE) == 0, (searchFlags & SPACE_DASH_EQUIVALENT) != 0, fileRef, wikiRef);
    }

    public static boolean endsWithWikiRef(int searchFlags, @NotNull String fileRef, @NotNull String wikiRef) {
        return FilePathInfo.endsWithWikiRef((searchFlags & CASE_INSENSITIVE) == 0, (searchFlags & SPACE_DASH_EQUIVALENT) != 0, fileRef, wikiRef);
    }

    public static boolean equivalentWikiRef(int searchFlags, @NotNull String fileRef, @NotNull String wikiRef) {
        return FilePathInfo.equivalentWikiRef((searchFlags & CASE_INSENSITIVE) == 0, (searchFlags & SPACE_DASH_EQUIVALENT) != 0, fileRef, wikiRef);
    }

    @Nullable
    protected static FileReferenceList.Filter getQueryFilter(FileReference sourceFileReference, String matchPattern, int searchFlags) {
        FileReferenceList.Filter filter;
        if (sourceFileReference == null) {
            // if match then it is the ending of the reference path
            if (matchPattern == null) {
                filter = null;
            } else {
                filter = getMatchAnyFileFilter(matchPattern, searchFlags);
            }
        } else {
            if (matchPattern == null) {
                filter = getAnyFileFilter(sourceFileReference);
            } else {
                filter = getMatchFileFilter(matchPattern, searchFlags, sourceFileReference);
            }
        }
        return filter;
    }

    @NotNull
    protected static FileReferenceList.Filter getAnyFileFilter(@NotNull final FileReference sourceFileReference) {
        return new FileReferenceList.Filter() {
            @Override
            public boolean filterExt(@NotNull String ext) {
                return true;
            }

            @Override
            public FileReference filterRef(@NotNull FileReference fileReference) {
                return new FileReferenceLink(sourceFileReference, fileReference);
            }
        };
    }

    @NotNull
    protected static FileReferenceList.Filter getMatchAnyFileFilter(@NotNull final String matchPattern, final int searchFlags) {
        FileReferenceList.Filter filter;

        switch (searchFlags & MATCH_TYPE_FLAGS) {
            case WIKIPAGE_REF:
                filter = new FileReferenceList.Filter() {
                    @Override
                    public boolean filterExt(@NotNull String ext) {
                        return equivalent(searchFlags, ext, "md");
                    }

                    @Override
                    public FileReference filterRef(@NotNull FileReference fileReference) {
                        return equivalentWikiRef(searchFlags, fileReference.getFileNameNoExtAsWikiRef(), matchPattern) ? fileReference : null;
                    }
                };
                break;

            case LINK_WITH_EXT_REF:
                filter = new FileReferenceList.Filter() {
                    @Override
                    public boolean filterExt(@NotNull String ext) {
                        return equivalent(searchFlags, ext, new FilePathInfo(matchPattern).getExt());
                    }

                    @Override
                    public FileReference filterRef(@NotNull FileReference fileReference) {
                        return equivalent(searchFlags, fileReference.getFileName(), matchPattern) ? fileReference : null;
                    }
                };
                break;

            default:
            case LINK_REF_NO_EXT:
                filter = new FileReferenceList.Filter() {
                    @Override
                    public boolean filterExt(@NotNull String ext) {
                        return true;
                    }

                    @Override
                    public FileReference filterRef(@NotNull FileReference fileReference) {
                        return equivalent(searchFlags, fileReference.getFileNameNoExt(), matchPattern) ? fileReference : null;
                    }
                };
                break;
        }
        return filter;
    }

    @NotNull
    protected static FileReferenceList.Filter getMatchFileFilter(@NotNull final String matchPattern, final int searchFlags, @NotNull final FileReference sourceFileReference) {
        FileReferenceList.Filter filter;
        switch (searchFlags & MATCH_TYPE_FLAGS) {
            case WIKIPAGE_REF:
                filter = new FileReferenceList.Filter() {
                    @Override
                    public boolean filterExt(@NotNull String ext) {
                        return true;
                    }

                    @Override
                    public FileReference filterRef(@NotNull FileReference fileReference) {
                        if (matchPattern.equals("sample-documents") && fileReference.getFileNameNoExt().equals("sample-documents")) {
                            int tmp = 0;
                        }
                        FileReferenceLink referenceLink = new FileReferenceLink(sourceFileReference, fileReference);
                        return equivalentWikiRef(searchFlags, referenceLink.getWikiPageRef(), matchPattern) && ((searchFlags & INCLUDE_SOURCE) != 0 || !fileReference.getFilePath().equals(sourceFileReference.getFilePath())) ? referenceLink : null;
                    }
                };
                break;

            default:
            case LINK_WITH_EXT_REF:
                filter = new FileReferenceList.Filter() {
                    @Override
                    public boolean filterExt(@NotNull String ext) {
                        return equivalent(searchFlags, ext, new FilePathInfo(matchPattern).getExt());
                    }

                    @Override
                    public FileReference filterRef(@NotNull FileReference fileReference) {
                        FileReferenceLink referenceLink = new FileReferenceLink(sourceFileReference, fileReference);
                        return equivalent(searchFlags, fileReference.getFilePath(), matchPattern) && ((searchFlags & INCLUDE_SOURCE) != 0 || !fileReference.getFilePath().equals(sourceFileReference.getFilePath())) ?
                                referenceLink : null;
                    }
                };
                break;

            case LINK_REF_NO_EXT:
                filter = new FileReferenceList.Filter() {
                    @Override
                    public boolean filterExt(@NotNull String ext) {
                        return true;
                    }

                    @Override
                    public FileReference filterRef(@NotNull FileReference fileReference) {
                        FileReferenceLink referenceLink = new FileReferenceLink(sourceFileReference, fileReference);
                        return equivalent(searchFlags, fileReference.getFilePathNoExt(), matchPattern) && ((searchFlags & INCLUDE_SOURCE) != 0 || !fileReference.getFilePath().equals(sourceFileReference.getFilePath())) ?
                                referenceLink : null;
                    }
                };
                break;
        }
        return filter;
    }
}