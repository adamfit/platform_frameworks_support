/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.app.slice;

import static android.app.slice.SliceItem.FORMAT_ACTION;
import static android.app.slice.SliceItem.FORMAT_IMAGE;
import static android.app.slice.SliceItem.FORMAT_INT;
import static android.app.slice.SliceItem.FORMAT_REMOTE_INPUT;
import static android.app.slice.SliceItem.FORMAT_SLICE;
import static android.app.slice.SliceItem.FORMAT_TEXT;
import static android.app.slice.SliceItem.FORMAT_TIMESTAMP;

import android.app.PendingIntent;
import android.app.RemoteInput;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.annotation.RestrictTo;
import android.support.annotation.RestrictTo.Scope;
import android.support.annotation.StringDef;
import android.text.TextUtils;
import android.util.Pair;

import java.util.Arrays;
import java.util.List;


/**
 * A SliceItem is a single unit in the tree structure of a {@link Slice}.
 * <p>
 * A SliceItem a piece of content and some hints about what that content
 * means or how it should be displayed. The types of content can be:
 * <li>{@link android.app.slice.SliceItem#FORMAT_SLICE}</li>
 * <li>{@link android.app.slice.SliceItem#FORMAT_TEXT}</li>
 * <li>{@link android.app.slice.SliceItem#FORMAT_IMAGE}</li>
 * <li>{@link android.app.slice.SliceItem#FORMAT_ACTION}</li>
 * <li>{@link android.app.slice.SliceItem#FORMAT_INT}</li>
 * <li>{@link android.app.slice.SliceItem#FORMAT_TIMESTAMP}</li>
 * <li>{@link android.app.slice.SliceItem#FORMAT_REMOTE_INPUT}</li>
 * <p>
 * The hints that a {@link SliceItem} are a set of strings which annotate
 * the content. The hints that are guaranteed to be understood by the system
 * are defined on {@link Slice}.
 */
public class SliceItem {

    private static final String HINTS = "hints";
    private static final String FORMAT = "format";
    private static final String SUBTYPE = "subtype";
    private static final String OBJ = "obj";
    private static final String OBJ_2 = "obj_2";

    /**
     * @hide
     */
    @RestrictTo(Scope.LIBRARY)
    @StringDef({FORMAT_SLICE, FORMAT_TEXT, FORMAT_IMAGE, FORMAT_ACTION, FORMAT_INT,
            FORMAT_TIMESTAMP, FORMAT_REMOTE_INPUT})
    public @interface SliceType {
    }

    /**
     * @hide
     */
    @RestrictTo(Scope.LIBRARY)
    protected @Slice.SliceHint String[] mHints;
    private final String mFormat;
    private final String mSubType;
    private final Object mObj;

    /**
     * @hide
     */
    @RestrictTo(Scope.LIBRARY)
    public SliceItem(Object obj, @SliceType String format, String subType,
            @Slice.SliceHint String[] hints) {
        mHints = hints;
        mFormat = format;
        mSubType = subType;
        mObj = obj;
    }

    /**
     * @hide
     */
    @RestrictTo(Scope.LIBRARY)
    public SliceItem(PendingIntent intent, Slice slice, String format, String subType,
            @Slice.SliceHint String[] hints) {
        this(new Pair<>(intent, slice), format, subType, hints);
    }

    /**
     * Gets all hints associated with this SliceItem.
     *
     * @return Array of hints.
     */
    public @NonNull @Slice.SliceHint List<String> getHints() {
        return Arrays.asList(mHints);
    }

    /**
     * @hide
     */
    @RestrictTo(Scope.LIBRARY)
    public void addHint(@Slice.SliceHint String hint) {
        mHints = ArrayUtils.appendElement(String.class, mHints, hint);
    }

    /**
     * @hide
     */
    @RestrictTo(Scope.LIBRARY)
    public void removeHint(String hint) {
        ArrayUtils.removeElement(String.class, mHints, hint);
    }

    /**
     * Get the format of this SliceItem.
     * <p>
     * The format will be one of the following types supported by the platform:
     * <li>{@link android.app.slice.SliceItem#FORMAT_SLICE}</li>
     * <li>{@link android.app.slice.SliceItem#FORMAT_TEXT}</li>
     * <li>{@link android.app.slice.SliceItem#FORMAT_IMAGE}</li>
     * <li>{@link android.app.slice.SliceItem#FORMAT_ACTION}</li>
     * <li>{@link android.app.slice.SliceItem#FORMAT_INT}</li>
     * <li>{@link android.app.slice.SliceItem#FORMAT_TIMESTAMP}</li>
     * <li>{@link android.app.slice.SliceItem#FORMAT_REMOTE_INPUT}</li>
     * @see #getSubType() ()
     */
    public @SliceType String getFormat() {
        return mFormat;
    }

    /**
     * Get the sub-type of this SliceItem.
     * <p>
     * Subtypes provide additional information about the type of this information beyond basic
     * interpretations inferred by {@link #getFormat()}. For example a slice may contain
     * many {@link android.app.slice.SliceItem#FORMAT_TEXT} items, but only some of them may be
     * {@link android.app.slice.Slice#SUBTYPE_MESSAGE}.
     * @see #getFormat()
     */
    public String getSubType() {
        return mSubType;
    }

    /**
     * @return The text held by this {@link android.app.slice.SliceItem#FORMAT_TEXT} SliceItem
     */
    public CharSequence getText() {
        return (CharSequence) mObj;
    }

    /**
     * @return The icon held by this {@link android.app.slice.SliceItem#FORMAT_IMAGE} SliceItem
     */
    @RequiresApi(23)
    public Icon getIcon() {
        return (Icon) mObj;
    }

    /**
     * @return The pending intent held by this {@link android.app.slice.SliceItem#FORMAT_ACTION}
     * SliceItem
     */
    public PendingIntent getAction() {
        return ((Pair<PendingIntent, Slice>) mObj).first;
    }

    /**
     * @return The remote input held by this {@link android.app.slice.SliceItem#FORMAT_REMOTE_INPUT}
     * SliceItem
     */
    @RequiresApi(20)
    public RemoteInput getRemoteInput() {
        return (RemoteInput) mObj;
    }

    /**
     * @return The color held by this {@link android.app.slice.SliceItem#FORMAT_INT} SliceItem
     */
    public int getInt() {
        return (Integer) mObj;
    }

    /**
     * @return The slice held by this {@link android.app.slice.SliceItem#FORMAT_ACTION} or
     * {@link android.app.slice.SliceItem#FORMAT_SLICE} SliceItem
     */
    public Slice getSlice() {
        if (FORMAT_ACTION.equals(getFormat())) {
            return ((Pair<PendingIntent, Slice>) mObj).second;
        }
        return (Slice) mObj;
    }

    /**
     * @return The timestamp held by this {@link android.app.slice.SliceItem#FORMAT_TIMESTAMP}
     * SliceItem
     */
    public long getTimestamp() {
        return (Long) mObj;
    }

    /**
     * @param hint The hint to check for
     * @return true if this item contains the given hint
     */
    public boolean hasHint(@Slice.SliceHint String hint) {
        return ArrayUtils.contains(mHints, hint);
    }

    /**
     * @hide
     */
    @RestrictTo(Scope.LIBRARY)
    public SliceItem(Bundle in) {
        mHints = in.getStringArray(HINTS);
        mFormat = in.getString(FORMAT);
        mSubType = in.getString(SUBTYPE);
        mObj = readObj(mFormat, in);
    }

    /**
     * @hide
     * @return
     */
    @RestrictTo(Scope.LIBRARY)
    public Bundle toBundle() {
        Bundle b = new Bundle();
        b.putStringArray(HINTS, mHints);
        b.putString(FORMAT, mFormat);
        b.putString(SUBTYPE, mSubType);
        writeObj(b, mObj, mFormat);
        return b;
    }

    /**
     * @hide
     */
    @RestrictTo(Scope.LIBRARY)
    public boolean hasHints(@Slice.SliceHint String[] hints) {
        if (hints == null) return true;
        for (String hint : hints) {
            if (!TextUtils.isEmpty(hint) && !ArrayUtils.contains(mHints, hint)) {
                return false;
            }
        }
        return true;
    }

    /**
     * @hide
     */
    @RestrictTo(Scope.LIBRARY)
    public boolean hasAnyHints(@Slice.SliceHint String... hints) {
        if (hints == null) return false;
        for (String hint : hints) {
            if (ArrayUtils.contains(mHints, hint)) {
                return true;
            }
        }
        return false;
    }

    private void writeObj(Bundle dest, Object obj, String type) {
        switch (type) {
            case FORMAT_IMAGE:
            case FORMAT_REMOTE_INPUT:
                dest.putParcelable(OBJ, (Parcelable) obj);
                break;
            case FORMAT_SLICE:
                dest.putParcelable(OBJ, ((Slice) obj).toBundle());
                break;
            case FORMAT_ACTION:
                dest.putParcelable(OBJ, ((Pair<PendingIntent, Slice>) obj).first);
                dest.putBundle(OBJ_2, ((Pair<PendingIntent, Slice>) obj).second.toBundle());
                break;
            case FORMAT_TEXT:
                dest.putCharSequence(OBJ, (CharSequence) obj);
                break;
            case FORMAT_INT:
                dest.putInt(OBJ, (Integer) mObj);
                break;
            case FORMAT_TIMESTAMP:
                dest.putLong(OBJ, (Long) mObj);
                break;
        }
    }

    private static Object readObj(String type, Bundle in) {
        switch (type) {
            case FORMAT_IMAGE:
            case FORMAT_REMOTE_INPUT:
                return in.getParcelable(OBJ);
            case FORMAT_SLICE:
                return new Slice(in.getBundle(OBJ));
            case FORMAT_TEXT:
                return in.getCharSequence(OBJ);
            case FORMAT_ACTION:
                return new Pair<>(
                        (PendingIntent) in.getParcelable(OBJ),
                        new Slice(in.getBundle(OBJ_2)));
            case FORMAT_INT:
                return in.getInt(OBJ);
            case FORMAT_TIMESTAMP:
                return in.getLong(OBJ);
        }
        throw new RuntimeException("Unsupported type " + type);
    }

    /**
     * @hide
     */
    @RestrictTo(Scope.LIBRARY)
    public static String typeToString(String format) {
        switch (format) {
            case FORMAT_SLICE:
                return "Slice";
            case FORMAT_TEXT:
                return "Text";
            case FORMAT_IMAGE:
                return "Image";
            case FORMAT_ACTION:
                return "Action";
            case FORMAT_INT:
                return "Int";
            case FORMAT_TIMESTAMP:
                return "Timestamp";
            case FORMAT_REMOTE_INPUT:
                return "RemoteInput";
        }
        return "Unrecognized format: " + format;
    }
}