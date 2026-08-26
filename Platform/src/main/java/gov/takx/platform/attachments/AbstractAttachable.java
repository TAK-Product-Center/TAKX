/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package gov.takx.platform.attachments;

import jakarta.annotation.Nonnull;

import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A thing which can have named attachments.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
@SuppressWarnings("unchecked")
public abstract class AbstractAttachable implements Attachable
{
    private Map<AttachmentKey<?>, Object> attachments;

    @Override
    public <T> T getAttachment(final AttachmentKey<T> key)
    {
        if (key == null || attachments == null)
        {
            return null;
        }
        return (T) attachments.get(key);
    }

    @Override
    public <T> List<T> getAttachmentList(AttachmentKey<? extends List<T>> key)
    {
        return (key == null || attachments == null)
                ? Collections.EMPTY_LIST
                : (List<T>) attachments.getOrDefault(key, Collections.EMPTY_LIST);
    }

    @Override
    public <T> Set<T> getAttachmentSet(AttachmentKey<? extends Set<T>> key)
    {
        return (key == null || attachments == null)
                ? Collections.EMPTY_SET
                : (Set<T>) attachments.getOrDefault(key, Collections.EMPTY_SET);
    }

    @Override
    public <T> T putAttachment(@Nonnull final AttachmentKey<T> key, final T value)
    {
        if (attachments == null) attachments = createAttachmentMap();

        return (T) attachments.put(key, value);
    }

    protected Map<AttachmentKey<?>, Object> createAttachmentMap()
    {
        return new IdentityHashMap<>();
    }

    @Override
    public <T> T removeAttachment(final AttachmentKey<T> key)
    {
        return (key == null || attachments == null)
                ? null
                : (T) attachments.remove(key);
    }

    @Override
    public <T> void addToAttachmentList(@Nonnull final AttachmentKey<AttachmentList<T>> key, final T value)
    {
        AttachmentList<T> attachmentList = getOrCreateAttachmentList(key);
        attachmentList.add(value);
    }

    /**
     * Gets the attachment list associated with the provided key, or creates one if it doesn't already exist.
     *
     * @param key the attachment key
     * @param <T> the list value type
     * @return The attachment list associated with the key
     */
    private <T> AttachmentList<T> getOrCreateAttachmentList(@Nonnull AttachmentKey<AttachmentList<T>> key)
    {
        if (attachments == null) attachments = createAttachmentMap();

        return (AttachmentList<T>) attachments.computeIfAbsent(key, k -> new AttachmentList<>(((ListAttachmentKey<T>) key).getValueClass()));
    }

    @Override
    public <T> void addAllToAttachmentList(@Nonnull AttachmentKey<AttachmentList<T>> key, Collection<T> values)
    {
        AttachmentList<T> attachmentList = getOrCreateAttachmentList(key);
        attachmentList.addAll(values);
    }

    @Override
    public <T> void addToAttachmentSet(@Nonnull final AttachmentKey<AttachmentSet<T>> key, final T value)
    {
        AttachmentSet<T> attachmentSet = getOrCreateAttachmentSet(key);
        attachmentSet.add(value);
    }

    /**
     * Gets the attachment set associated with the provided key, or creates one if it doesn't already exist.
     *
     * @param key the attachment key
     * @param <T> the set value type
     * @return The attachment set associated with the key
     */
    private <T> AttachmentSet<T> getOrCreateAttachmentSet(@Nonnull AttachmentKey<AttachmentSet<T>> key)
    {
        if (attachments == null) attachments = createAttachmentMap();

        return (AttachmentSet<T>) attachments.computeIfAbsent(key, k -> new AttachmentSet<>(((SetAttachmentKey<T>) key).getValueClass()));
    }

    @Override
    public <T> void addAllToAttachmentSet(@Nonnull final AttachmentKey<AttachmentSet<T>> key, final Collection<T> values)
    {
        AttachmentSet<T> attachmentSet = getOrCreateAttachmentSet(key);
        attachmentSet.addAll(values);
    }
}
