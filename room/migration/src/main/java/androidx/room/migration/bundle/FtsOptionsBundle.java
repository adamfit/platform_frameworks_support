/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.room.migration.bundle;

import androidx.annotation.RestrictTo;
import androidx.room.Fts3Entity;
import androidx.room.Fts4Entity;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Data class that holds FTS Options of an {@link Fts3Entity Fts3Entity} or
 * {@link Fts4Entity Fts4Entity}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class FtsOptionsBundle implements SchemaEquality<FtsOptionsBundle> {

    @SerializedName("tokenizer")
    private final String mTokenizer;

    @SerializedName("tokenizerArgs")
    private final List<String> mTokenizerArgs;

    @SerializedName("languageIdColumnName")
    private final String mLanguageIdColumnName;

    @SerializedName("matchInfo")
    private final String mMatchInfo;

    @SerializedName("notIndexedColumns")
    private final List<String> mNotIndexedColumns;

    @SerializedName("prefixSizes")
    private final List<Integer> mPrefixSizes;

    @SerializedName("preferredOrder")
    private final String mPreferredOrder;

    public FtsOptionsBundle(
            String tokenizer,
            List<String> tokenizerArgs,
            String languageIdColumnName,
            String matchInfo,
            List<String> notIndexedColumns,
            List<Integer> prefixSizes,
            String preferredOrder) {
        mTokenizer = tokenizer;
        mTokenizerArgs = tokenizerArgs;
        mLanguageIdColumnName = languageIdColumnName;
        mMatchInfo = matchInfo;
        mNotIndexedColumns = notIndexedColumns;
        mPrefixSizes = prefixSizes;
        mPreferredOrder = preferredOrder;
    }

    @Override
    public boolean isSchemaEqual(FtsOptionsBundle other) {
        return mTokenizer.equals(other.mTokenizer)
                && mTokenizerArgs.equals(other.mTokenizerArgs)
                && mLanguageIdColumnName.equals(other.mLanguageIdColumnName)
                && mMatchInfo.equals(other.mMatchInfo)
                && mNotIndexedColumns.equals(other.mNotIndexedColumns)
                && mPrefixSizes.equals(other.mPrefixSizes)
                && mPreferredOrder.equals(other.mPreferredOrder);

    }
}
