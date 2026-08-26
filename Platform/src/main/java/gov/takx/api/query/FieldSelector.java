package gov.takx.api.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * This class provides a unified way to specify core, extended and aggregate fields when working with the
 * query service.
 * <p/>
 * <em>Note that the name conflict/similarity with the {@link AFieldSelector} hierarchy is unfortunate.  However, we
 * had to maintain backward compatibility with the existing API.  We could have chosen a different name pattern, like
 * "*Specifier", but we felt that the name should indicate the function, so we stuck with "*Selector" and apologize
 * for any confusion that may cause.  It may be best to think of this class as a factory class for the
 * *Selector classes.</em>
 * <p/>
 * Typical usage is to create the bare object and then use the addXXX methods to specify the
 * core, extended and aggregate fields to select.  The add methods return this object so they can be chained
 * (similar to {@code StringBuffer.append)}.
 * <p/>
 * Note that the extended fields may only come from a single structure.  This follows from the fact that no RDM
 * can be defined by more than one structure.
 * <p/>
 * Query results for each matching RDM will contain core fields first, in the order specified to addCoreFields,
 * followed by extended fields, in the order you specified them to addExtendedFields and, finally, the aggregate
 * fields, in the order specified to addAggregateFields.  For example, assuming a message structure named
 * STATUS_MESSAGE with a field {@code key} of type {@code String}, and a field {@code voltage} of
 * type {@code double}, the following field selector:
 * <pre>
 * new FieldSelector()
 *       .addCoreFields(CoreField.heading, CoreField.id)
 *       .setStructure("STATUS_MESSAGE").addExtendedFields("key", "voltage");
 * </pre>
 * will return a List of results, where each result is an array ordered like this:
 * <pre>
 * heading: Double
 * id:      Integer
 * key:     String
 * voltage: Double
 * </pre>
 * <p/>
 * As of version 1.49, a new selector and constraint object model were introduced.  All things that can be "selected"
 * are represented by some subclass of {@link AFieldSelector}.  Those selectors are fully qualified in that they
 * contain all the data needed to produce the SQL for referencing them.  The older API allowed the use of arbitrary
 * structure field names without specifying the structure name at the same time.  It was assumed that the structure
 * name would be set via some other call into the API.
 * <p/>
 * The new constraints hierarchy, rooted at {@link ComparisonConstraint}, use a selector to specify the DB content
 * to be constrained and a comparison type/value.  Again, since a constraint is created on a selector, the entire
 * construct is fully specified at creation time.
 * <p/>
 * It is a common idiom to create the selector, then create the constraints on those selectors.  To support a fluent
 * programming style, a new {@link #addSelectors(AFieldSelector...)} method has been added here.
 * <p/>
 * The older {@link #addCoreFields(CoreField...)} and {@link #addExtendedFields(String...)} are not deprecated yet, but
 * we encourage the use of the newer API.
 *
 * @see IQueryService#queryForFields(IQueryConstraints, FieldSelector)
 * @since 1.23
 */
@SuppressWarnings("UnusedDeclaration")
public class FieldSelector
{
    private String structure;
    private final List<CoreField> coreFields = new ArrayList<>();
    private final List<String> extendedFields = new ArrayList<>();

    // We could store all the selectors in a common collection, but the code to retrieve the properly
    // type sub-collections is a little gross.  So, instead, we have a lot less gross code and separate the
    // selectors into hard-typed lists.
    private final List<CoreFieldSelector> coreFieldSelectors = new ArrayList<>();
    private final List<StructureFieldSelector> extendedFieldSelectors = new ArrayList<>();
    private final List<AggregateFieldSelector> aggregateFieldSelectors = new ArrayList<>();

    /**
     * Constructor.
     */
    public FieldSelector()
    {
    }

    /**
     * Add the list of core fields to this selector.
     *
     * @param fields Core fields to add
     * @return this object for chaining
     */
    public FieldSelector addCoreFields(CoreField... fields)
    {
        Collections.addAll(coreFields, fields);

        for (CoreField field : fields)
        {
            coreFieldSelectors.add(new CoreFieldSelector(field));
        }

        return this;
    }

    /**
     * Add a collection of field selectors, may be any of core, structure or aggregate selectors.
     *
     * @param selectors Selectors to add
     * @return this object for chaining
     * @throws ConflictingConstraintException If there is a structure conflict on an extended field selector
     */
    public FieldSelector addSelectors(AFieldSelector... selectors) throws ConflictingConstraintException
    {
        updateStructureFromSelector(selectors);

        // I don't like this, but I couldn't find a good way to have a single collection of the selectors
        // without having equally gross code when extracting typed subsets.  So, we
        for (AFieldSelector selector : selectors)
        {
            final Class<? extends AFieldSelector> cls = selector.getClass();

            if (cls == CoreFieldSelector.class)
            {
                final CoreFieldSelector cfs = (CoreFieldSelector) selector;
                coreFields.add(cfs.getCoreField());
                coreFieldSelectors.add(cfs);
            } else if (cls == StructureFieldSelector.class)
            {
                extendedFields.add(selector.getFieldName());
                extendedFieldSelectors.add((StructureFieldSelector) selector);
            } else if (cls == AggregateFieldSelector.class)
            {
                aggregateFieldSelectors.add((AggregateFieldSelector) selector);
            }
        }

        return this;
    }

    /**
     * Add an aggregate field selection on a core field.
     *
     * @param type      The aggregate type
     * @param coreField The core field to aggregate
     * @return this object for chaining
     */
    public FieldSelector addAggregateField(AggregateType type, CoreField coreField) throws ConflictingConstraintException
    {
        return addSelectors(new AggregateFieldSelector(type, coreField.selector));
    }

    /**
     * Add an aggregate field selection on a core field.
     *
     * @param type      The aggregate type
     * @param distinct  Pass {@code true} to aggregate distinct values
     * @param coreField The core field to aggregate
     * @return this object for chaining
     */
    public FieldSelector addAggregateField(AggregateType type, boolean distinct, CoreField coreField) throws ConflictingConstraintException
    {
        return addSelectors(new AggregateFieldSelector(type, distinct, coreField.selector));
    }

    /**
     * Add an aggregate field selection on a structure field.  The structure must have previously been set using a
     * call to {@link #setStructure(String)}.
     * <p/>
     * This is a convenience method replacing the following idiom:
     * <pre>
     *     addAggregateFields(new AggregateField(type, structure, fieldName)
     * </pre>
     *
     * @param type      The aggregate type
     * @param fieldName The field name on the structure to aggregate
     * @return this object for chaining
     * @throws IllegalStateException If the structure has not been previously set
     * @see #setStructure(String)
     */
    public FieldSelector addAggregateField(AggregateType type, String fieldName) throws IllegalStateException, ConflictingConstraintException
    {
        return addAggregateField(type, false, new StructureFieldSelector(structure, fieldName));
    }

    /**
     * Add an aggregate field selection on a structure field.  The fieldAlias of the structure field is respected.
     * The structure must have previously been set using a call to {@link #setStructure(String)}.
     * <p/>
     * This is a convenience method replacing the following idiom:
     * <pre>
     *     addAggregateFields(new AggregateField(type, structure, fieldName)
     * </pre>
     *
     * @param type  The aggregate type
     * @param field The field on the structure to aggregate
     * @return this object for chaining
     * @throws IllegalStateException If the structure has not been previously set or if the field's structure differs
     *                               from this selector's structure.
     * @see #setStructure(String)
     */
    public FieldSelector addAggregateField(AggregateType type, StructureFieldSelector field)
            throws IllegalStateException, ConflictingConstraintException
    {
        return addAggregateField(type, false, field);
    }

    /**
     * Add an aggregate field selection on a structure field.  The structure must have previously been set using a
     * call to {@link #setStructure(String)}.
     * <p/>
     * This is a convenience method replacing the following idiom:
     * <pre>
     *     addAggregateFields(new AggregateField(type, distinct, structure, fieldName)
     * </pre>
     *
     * @param type      The aggregate type
     * @param distinct  Pass {@code true} to aggregate distinct values
     * @param fieldName The field name on the structure to aggregate
     * @return this object for chaining
     * @throws IllegalStateException If the structure has not been previously set
     * @see #setStructure(String)
     */
    public FieldSelector addAggregateField(AggregateType type, boolean distinct, String fieldName) throws IllegalStateException, ConflictingConstraintException
    {
        return addAggregateField(type, distinct, new StructureFieldSelector(structure, fieldName));
    }

    /**
     * Add an aggregate field selection on a structure field.  The fieldAlias of the structure field is respected.
     * The structure must have previously been set using a call to {@link #setStructure(String)}.
     * <p/>
     * This is a convenience method replacing the following idiom:
     * <pre>
     *     addAggregateFields(new AggregateField(type, distinct, structure, fieldName)
     * </pre>
     *
     * @param type     The aggregate type
     * @param distinct Pass {@code true} to aggregate distinct values
     * @param field    The field on the structure to aggregate
     * @return this object for chaining
     * @throws IllegalStateException If the structure has not been previously set or if the field's structure differs
     *                               from this selector's structure.
     * @see #setStructure(String)
     */
    public FieldSelector addAggregateField(AggregateType type, boolean distinct, StructureFieldSelector field) throws IllegalStateException, ConflictingConstraintException
    {
        if (structure == null) throw new IllegalStateException("No structure specified via setStructure()");
        if (!structure.equals(field.structure))
        {
            throw new IllegalStateException("Structure for aggregate field differs from FieldSelector structure (via setStructure)");
        }

        return addSelectors(new AggregateFieldSelector(type, distinct, field));
    }

    /**
     * Set the name of the structure in which the extended fields are defined.
     *
     * @param structure Name of the defining structure
     * @return this object for chaining
     * @throws IllegalStateException If a different structure has already been specified
     */
    public FieldSelector setStructure(String structure)
    {
        if (this.structure != null && !this.structure.equals(structure))
        {
            throw new IllegalStateException("A structure has already been set: " + this.structure);
        }

        this.structure = structure;
        return this;
    }

    /**
     * Add the list of extended fields to this selector.  Note that if you fail to define a structure using
     * {@link #setStructure(String)}, an {@link InvalidFieldSelectorException} will be thrown when this
     * selector is used in a query.
     *
     * @param fields extended field names to add
     * @return this object for chaining
     */
    public FieldSelector addExtendedFields(String... fields)
    {
        Collections.addAll(extendedFields, fields);

        for (String field : fields)
        {
            extendedFieldSelectors.add(new StructureFieldSelector(structure, field));
        }

        return this;
    }

    /**
     * Add the list of StructureFieldSelector fields to this selector.  By adding a StructureFieldSelector,
     * as opposed to simply the selector's fieldName, one maintains the selector's fieldAlias.  Note that if you fail
     * to define a structure using {@link #setStructure(String)}, an {@link InvalidFieldSelectorException} will be
     * thrown when this selector is used in a query.
     *
     * @param fields extended fields to add
     * @return this object for chaining
     */
    public FieldSelector addExtendedFields(StructureFieldSelector... fields)
    {
        Stream.of(fields)
                .forEach(field -> {
                    extendedFields.add(field.getFieldName());
                    extendedFieldSelectors.add(field);
                });
        return this;
    }

    /**
     * @return {@code true} if this selector contains any core fields, {@code false} if not
     */
    public boolean hasCoreFields()
    {
        return !coreFields.isEmpty();
    }

    /**
     * @return {@code true} if this selector contains any aggregate fields, otherwise @{code false}.
     */
    public boolean hasAggregateFields()
    {
        return !aggregateFieldSelectors.isEmpty();
    }

    /**
     * Determine if any extended fields have been added.  Note, this is specifically talking about calls to
     * {@link #addExtendedFields(String...)} or {@link #addSelectors(AFieldSelector...)} where a
     * {@link StructureFieldSelector} was in the parameter list.  It is not testing to see if an extended field is
     * being referenced on any selector, that's the job of {@link #usesExtendedFields()}.
     *
     * @return {@code true} if this selector contains any extended fields, {@code false} if not
     * @see #usesExtendedFields()
     */
    public boolean hasExtendedFields()
    {
        return !extendedFields.isEmpty();
    }

    /**
     * @return {@code true} if any selector references an extended field
     */
    public boolean usesExtendedFields()
    {
        if (!extendedFieldSelectors.isEmpty()) return true;

        for (AggregateFieldSelector field : aggregateFieldSelectors)
        {
            if (!field.isCoreField())
            {
                return true;
            }
        }
        return false;
    }

    /**
     * @return The list of core fields, may be empty
     */
    public List<CoreField> getCoreFields()
    {
        return coreFields;
    }

    /**
     * @return The list of core field selectors
     */
    public List<CoreFieldSelector> getCoreFieldSelectors()
    {
        return coreFieldSelectors;
    }

    /**
     * @return The list of aggregate fields, may be empty
     */
    public List<AggregateFieldSelector> getAggregateFieldSelectors()
    {
        return aggregateFieldSelectors;
    }

    /**
     * @return The list of extended field names, may be empty
     */
    public List<String> getExtendedFields()
    {
        return extendedFields;
    }

    /**
     * @return The list of extended field selectors, may be empty
     */
    public List<StructureFieldSelector> getExtendedFieldSelectors()
    {
        return extendedFieldSelectors;
    }

    /**
     * @return The structure, {@code null} if not set
     */
    public String getStructure()
    {
        return structure;
    }

    /**
     * Determine if this selector has any fields actually selected.
     *
     * @return {@code true} if any fields have been added to this selector
     */
    public boolean isEmpty()
    {
        return !hasCoreFields() && !hasExtendedFields() && !hasAggregateFields();
    }

    /**
     * Validate all the selectors against the current structure, update structure if this is the first use of an
     * extended field.
     *
     * @param selectors Selectors to process
     * @throws ConflictingConstraintException
     */
    private void updateStructureFromSelector(AFieldSelector... selectors) throws ConflictingConstraintException
    {
        for (AFieldSelector selector : selectors)
        {
            if (!selector.isCoreField())
            {
                final String s = selector.getStructureName();

                if (structure == null)
                {
                    structure = s;
                } else
                {
                    // Ensure we don't reference multiple structures
                    if (!structure.equals(s))
                    {
                        throw new ConflictingConstraintException("Multiple structures referenced: " + structure + " and " + s);
                    }
                }
            }
        }
    }
}
