package gov.takx.dataservices.utils;

import gov.takx.api.annotation.Internal;
import gov.takx.api.messages.IDataField;
import gov.takx.api.messages.IDataType;
import gov.takx.api.messages.IExtendedDataStructure;
import gov.takx.api.messages.IStructure;
import gov.takx.core.persist.PostgresCopyUtils;
import gov.takx.dataservices.persist.AExtendedDataStructure;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import javassist.CannotCompileException;
import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtNewMethod;
import javassist.LoaderClassPath;
import javassist.NotFoundException;

/**
 * This class handles the dynamic generation of classes used to hold the structure data for an RDM.
 * 
 * @see DynamicStructureDBUtils
 */
@SuppressWarnings("NonConstantFieldWithUpperCaseName")
@Internal
@ApplicationScoped
public class DynamicStructures
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final int[] PRIMITIVE_FIELD_TYPES = {IDataType.BOOLEAN, IDataType.SHORT, IDataType.INT, IDataType.LONG, IDataType.FLOAT, IDataType.DOUBLE};

    // Map from field data type to method to cast an object to a wrapper type and then un-box the value
    private static final Map<Integer, String> DATA_TYPE_TO_CAST_UNBOX = Map.of(
            IDataType.BOOLEAN, "((Boolean)_$_value).booleanValue()",
            IDataType.DOUBLE, "((Double)_$_value).doubleValue()",
            IDataType.FLOAT, "((Float)_$_value).floatValue()",
            IDataType.INT, "((Integer)_$_value).intValue()",
            IDataType.LONG, "((Long)_$_value).longValue()",
            IDataType.SHORT, "((Short)_$_value).shortValue()");

    private static final Map<Integer, String> DATA_TYPE_TO_PRIMITIVE_SETTER = Map.of(
            IDataType.BOOLEAN, "setBoolean",
            IDataType.DOUBLE, "setDouble",
            IDataType.FLOAT, "setFloat",
            IDataType.INT, "setInt",
            IDataType.LONG, "setLong",
            IDataType.SHORT, "setShort");

    private static final Map<Integer, String> DATA_TYPE_TO_PRIMITIVE_GETTER = Map.of(
            IDataType.BOOLEAN, "getBoolean",
            IDataType.DOUBLE, "getDouble",
            IDataType.FLOAT, "getFloat",
            IDataType.INT, "getInt",
            IDataType.LONG, "getLong",
            IDataType.SHORT, "getShort");

    private static final Map<Integer, String> DATA_TYPE_TO_ARRAY_UTIL = Map.of(
            IDataType.STRING_ARRAY, "stringArrayFromBytes",
            IDataType.BOOLEAN_ARRAY, "boolArrayFromBytes",
            IDataType.INT_ARRAY, "intArrayFromBytes",
            IDataType.FLOAT_ARRAY, "floatArrayFromBytes",
            IDataType.SHORT_ARRAY, "shortArrayFromBytes",
            IDataType.LONG_ARRAY, "longArrayFromBytes",
            IDataType.DOUBLE_ARRAY, "doubleArrayFromBytes",
            IDataType.DATE_ARRAY, "dateArrayFromBytes");

    private static final String INSERT_SQL = "INSERT_SQL";
    private static final String SELECT_SQL = "SELECT_SQL";
    private static final String COPY_SQL = "COPY_SQL";

    @SuppressWarnings("NonFinalStaticVariableUsedInClassInitialization")
    private static final CtClass BOOLEAN_TYPE = CtClass.booleanType;

    // Modifier combination defining a "constant" field
    private static final int CONSTANT_MODIFIERS = Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL;

    // Tracks the set of classes that have been generated
    private static final Map<String, Class<? extends AExtendedDataStructure>> generatedClasses = new ConcurrentHashMap<>();

    // Map from class name to INSERT_SQL field declared on that class
    private static final Map<String, Field> insertSqlFieldMap = new HashMap<>();

    // Map from class name to SELECT_SQL field declared on that class
    private static final Map<String, Field> selectSqlFieldMap = new HashMap<>();

    // Map from class name to COPY_SQL field declared on that class
    private static final Map<String, Field> copySqlFieldMap = new HashMap<>();

    private static final ClassPool classPool; // The class pool used to generate classes

    private static CtClass SUPER_CLASS;

    static
    {
        // Get and initialize the dynamic class pool
        // Ensure that it has access to all the classes that this class does

        classPool = ClassPool.getDefault();
        classPool.insertClassPath(new ClassClassPath(DynamicStructures.class));
        classPool.insertClassPath(new LoaderClassPath(DynamicStructures.class.getClassLoader()));
        classPool.importPackage("gov.takx.dataservices.persist");
        classPool.importPackage("gov.takx.core.persist");
        classPool.importPackage("gov.takx.api.messages");
        classPool.importPackage("gov.takx.platform.utils");
        classPool.importPackage("jakarta.persistence");
        classPool.importPackage("java.sql");

        try
        {
            // The super class of all generated dynamic structure classes
            SUPER_CLASS = classPool.get(AExtendedDataStructure.class.getCanonicalName());
        } catch (NotFoundException e)
        {
            logger.error("Failed to locate required class; can't create dynamic structures!", e);
        }
    }

    DynamicStructures()
    {
    }

    /**
     * @return The class pool used to generate the extended data classes
     */
    static ClassPool getClassPool()
    {
        return classPool;
    }

    /**
     * Get or generate the extended data class for the given structure.
     *
     * @param structure Structure defining the class to be generated
     * @return Generated class for the given structure
     * @since 2.14
     */
    public Class<? extends AExtendedDataStructure> getOrGenerateClass(IStructure structure)
    {
        final String className = structure.getExtendedDataClassName();

        return generatedClasses.computeIfAbsent(className, k -> {
            logger.debug("Generating class {}", className);
            return generateClass(className, structure);
        });
    }

    /**
     * Generate the class for the given structure using the given class name.
     *
     * @param className Name of class to generate
     * @param structure Structure defining the fields that will become members on the class
     * @return Class created or {@code null} on failure
     */
    private Class<? extends AExtendedDataStructure> generateClass(String className, IStructure structure)
    {
        logger.debug("Generating class {}", className);

        try
        {
            final CtClass ctClass = classPool.makeClass(className);
            ctClass.setSuperclass(SUPER_CLASS);

            // Add all the fields for the Structure. Sort these by field name, so we have a consistent ordering.

            final List<IDataField> fields = new ArrayList<>(structure.getFields());
            fields.sort(Comparator.comparing(IDataField::getNeutralizedFieldName));

            createFields(ctClass, fields);

            // Add in persistence support
            final String columns = fields.isEmpty()
                    ? "mappoint_id"
                    : fields.stream().map(IDataField::getNeutralizedFieldName).collect(Collectors.joining(", ")) + ", mappoint_id";

            createInsertSqlConstant(ctClass, fields, columns);
            createSelectSqlConstant(ctClass, columns);
            createCopySqlConstant(ctClass, columns);
            createBindFieldValues(ctClass, fields);
            createLoadFieldValues(ctClass, fields);
            createBuildCopyString(ctClass, fields);

            createExtendedDataStructureMethods(ctClass, fields);

            // Generate and finalize the class
            final Class<? extends AExtendedDataStructure> generatedClass =
                    ctClass.toClass(getClass().getClassLoader(), null)
                            .asSubclass(AExtendedDataStructure.class);

            // Grab the INSERT_SQL and SELECT_SQL fields on this new class
            insertSqlFieldMap.put(className, generatedClass.getDeclaredField(INSERT_SQL));
            selectSqlFieldMap.put(className, generatedClass.getDeclaredField(SELECT_SQL));
            copySqlFieldMap.put(className, generatedClass.getDeclaredField(COPY_SQL));

            return generatedClass; // All good
        } catch (Throwable e)
        {
            logger.error("Failed to generate class {} for {}", className, structure, e);
        }

        return null;
    }

    /**
     * Create all the fields (members) to hold the data for the fields defined in the structure.
     *
     * @param ctClass The class being constructed
     * @param fields  The ordered list of structure fields to process
     * @since 2.14
     */
    private void createFields(CtClass ctClass, List<IDataField> fields) throws NotFoundException, CannotCompileException
    {
        for (IDataField f : fields)
        {
            final CtField ctField = createField(f, ctClass);

            if (ctField != null)
            {
                ctClass.addField(ctField);

                if (f.isPrimitive())
                {
                    createPrimitiveAccessors(f, ctClass);
                } else
                {
                    createReferenceAccessors(f, ctClass, ctField);
                }
            } else
            {
                logger.error("unknown data type {} for field {} on structure {}",
                        f.getDataTypeName(), f.getFieldName(), f.getStructure());
            }
        }
    }

    /**
     * Create a field object for the specified data field.
     *
     * @param field   The field from the structure to process
     * @param ctClass Containing structure class
     * @return the new CtField
     * @throws NotFoundException      If we fail to find the type or annotation
     * @throws CannotCompileException If the field creation fails
     */
    private CtField createField(IDataField field, CtClass ctClass)
            throws NotFoundException, CannotCompileException
    {
        // The property/member name is the field name with any special characters removed.
        final String fieldName = field.getNeutralizedFieldName();

        // Get the java field type for a given data field type
        final CtClass javaFieldType;
        final String typeClassName = field.getJavaType().getCanonicalName();

        if (typeClassName != null)
        {
            javaFieldType = classPool.get(typeClassName);
        } else
        {
            logger.error("Unknown type on field: {}", field);
            return null;
        }

        final CtField ctField = new CtField(javaFieldType, fieldName, ctClass);
        ctField.setModifiers(Modifier.PUBLIC);

        return ctField;
    }

    /**
     * Create the getter and setter methods for a primitive field.  Primitive fields require that we create a separate
     * field to track their "null" state, since it can't be directly recorded in the primitive value.
     *
     * @param dataField The field for which we are creating the getter/setter methods
     * @param ctClass   The class being constructed
     * @since 2.14
     */
    private void createPrimitiveAccessors(IDataField dataField, CtClass ctClass) throws CannotCompileException
    {
        final String fieldName = dataField.getNeutralizedFieldName();
        final int fieldType = dataField.getFieldType();

        logger.trace("Add accessors for primitive field: {}", fieldName);

        // Construct the 'null' state tracking field
        final String nullStateName = getNullStateName(fieldName);

        final CtField nullStateField = new CtField(BOOLEAN_TYPE, nullStateName, ctClass);
        ctClass.addField(nullStateField, CtField.Initializer.constant(true)); // Initialize to true

        // Make the isNull method
        final String isNullMethodName = getIsNullMethodName(dataField);
        ctClass.addMethod(CtNewMethod.getter(isNullMethodName, nullStateField));

        // Make the setNull method
        final String setNullMethodName = getSetNullMethodName(dataField);
        final String setNullBody = String.format("public void %s() { %s = true; }", setNullMethodName, nullStateName);
        logger.trace("   setNull {}: {}", fieldName, setNullBody);
        ctClass.addMethod(CtNewMethod.make(setNullBody, ctClass));

        // Construct a type-specific getter/setter since we have converted to using MethodHandle.invoke, which
        // allows us to avoid the wrapper class creation.
        final String typeName = dataField.getJavaType().getSimpleName();

        // Make the getter

        final String getterMethodName = getGetterMethodName(dataField);
        final String nullValue = fieldType == IDataField.BOOLEAN ? "false" : "0"; // False or 0 if null
        final String getterBody = String.format("public %s %s() { return %s ? %s : %s; }",
                typeName, getterMethodName, nullStateName, nullValue, fieldName);
        logger.trace("   getter {}: {}", fieldName, getterBody);
        ctClass.addMethod(CtNewMethod.make(getterBody, ctClass));

        // make the setter

        final String setterMethodName = getSetterMethodName(dataField);
        final String setterBody = String.format(
                "public void %1$s(%2$s _$_val) { " +
                        "%3$s = false; " +
                        "this.%4$s = _$_val;" +
                        "}",
                setterMethodName, typeName, nullStateName, fieldName);

        // An example of the filled out setter body (for an 'int' type field):
        // Note, the use of "_$_val" as the argument name is to ensure it can never shadow an actual field name
        //   public void set_someField(int _$_val) {
        //       _isNull_someField = false;
        //       this.someField = _$_val;
        //   }

        logger.trace("   setter {}: {}", fieldName, setterBody);
        ctClass.addMethod(CtNewMethod.make(setterBody, ctClass));
    }

    /**
     * Create the getter and setter methods for a reference field.  Reference fields handle all non-primitive
     * types and do not need null state tracking since they can be set to {@code null}.
     *
     * @param dataField The field for which we are creating the getter/setter methods
     * @param ctClass   The class being constructed
     * @param ctField   The field for which to create a getter
     * @since 2.14
     */
    private void createReferenceAccessors(IDataField dataField, CtClass ctClass, CtField ctField) throws CannotCompileException
    {
        final String fieldName = dataField.getNeutralizedFieldName();
        logger.trace("Add accessors for reference field: {}", fieldName);

        final String getterMethodName = getGetterMethodName(dataField);
        ctClass.addMethod(CtNewMethod.getter(getterMethodName, ctField));

        final String setterMethodName = getSetterMethodName(dataField);
        ctClass.addMethod(CtNewMethod.setter(setterMethodName, ctField));

        final String isNullMethodName = getIsNullMethodName(dataField);
        final String isNullBody = String.format("public boolean %s() { return %s == null; }", isNullMethodName, fieldName);
        logger.trace("   isNull {}: {}", fieldName, isNullBody);
        ctClass.addMethod(CtNewMethod.make(isNullBody, ctClass));

        final String setNullMethodName = getSetNullMethodName(dataField);
        final String setNullBody = String.format("public void %s() { %s = null; }", setNullMethodName, fieldName);
        logger.trace("   setNull {}: {}", fieldName, setNullBody);
        ctClass.addMethod(CtNewMethod.make(setNullBody, ctClass));
    }

    /**
     * Create all the getter/setter methods defined in the {@link IExtendedDataStructure}.
     *
     * @param ctClass The class being constructed
     * @param fields  The ordered list of structure fields to process
     * @since 2.31
     */
    private void createExtendedDataStructureMethods(CtClass ctClass, List<IDataField> fields) throws CannotCompileException
    {
        createPrimitiveGetterMethods(ctClass, fields);
        createPrimitiveSetterMethods(ctClass, fields);
        createGetFieldMethod(ctClass, fields);
        createSetFieldMethod(ctClass, fields);
        createIsNullMethod(ctClass, fields);
    }

    /**
     * Create all the primitive getter methods defined in {@link IExtendedDataStructure}.
     *
     * @param ctClass The class being constructed
     * @param fields  The ordered list of structure fields to process
     * @see IExtendedDataStructure#getBoolean(IDataField)
     * @see IExtendedDataStructure#getDouble(IDataField)
     * @see IExtendedDataStructure#getFloat(IDataField)
     * @see IExtendedDataStructure#getInt(IDataField)
     * @see IExtendedDataStructure#getLong(IDataField)
     * @see IExtendedDataStructure#getShort(IDataField)
     * @since 2.31
     */
    private void createPrimitiveGetterMethods(CtClass ctClass, List<IDataField> fields) throws CannotCompileException
    {
        for (int returnFieldType : PRIMITIVE_FIELD_TYPES)
        {
            final String returnType = IDataType.JAVA_TYPES[returnFieldType].getSimpleName();
            final String methodName = DATA_TYPE_TO_PRIMITIVE_GETTER.get(returnFieldType);
            final String methodDef = String.format("public %s %s(IDataField dataField)", returnType, methodName);
            final String nullValue = returnFieldType == IDataField.BOOLEAN ? "false" : "0"; // False or 0 if null

            generateFieldSwitchMethodBody(methodName, methodDef, ctClass, fields, true, returnType,
                    (dataField, localFieldName) -> {
                        return isLegalConversion(dataField.getFieldType(), returnFieldType)
                                ? String.format("return %1$s ? %2$s : (%3$s) %4$s;\n", getNullStateName(localFieldName), nullValue, returnType, localFieldName)
                                : null; // Not our type, don't generate a case
                    });
        }
    }

    /**
     * Determine if the given source type can be converted to the target type in a legal widening conversion.
     *
     * @param sourceType Source data type
     * @param targetType Target data type
     * @return {@code true} if the conversion is a legal widening conversion according to the JLS
     * @since 2.31
     */
    private boolean isLegalConversion(int sourceType, int targetType)
    {
        if (sourceType == targetType) return true; // Same type is always legal

        return switch (sourceType)
        {
            case IDataType.SHORT -> targetType == IDataType.INT || targetType == IDataType.LONG || targetType == IDataType.FLOAT || targetType == IDataType.DOUBLE;
            case IDataType.INT -> targetType == IDataType.LONG || targetType == IDataType.FLOAT || targetType == IDataType.DOUBLE;
            case IDataType.LONG -> targetType == IDataType.FLOAT || targetType == IDataType.DOUBLE;
            case IDataType.FLOAT -> targetType == IDataType.DOUBLE;
            default -> false;
        };
    }

    /**
     * Create all the primitive setter methods defined in {@link IExtendedDataStructure}.
     *
     * @param ctClass The class being constructed
     * @param fields  The ordered list of structure fields to process
     * @see IExtendedDataStructure#setBoolean(IDataField, boolean)
     * @see IExtendedDataStructure#setDouble(IDataField, double)
     * @see IExtendedDataStructure#setFloat(IDataField, float)
     * @see IExtendedDataStructure#setInt(IDataField, int)
     * @see IExtendedDataStructure#setLong(IDataField, long)
     * @see IExtendedDataStructure#setShort(IDataField, short)
     * @since 2.31
     */
    private void createPrimitiveSetterMethods(CtClass ctClass, List<IDataField> fields) throws CannotCompileException
    {
        for (int fieldType : PRIMITIVE_FIELD_TYPES)
        {
            final String valueType = IDataType.JAVA_TYPES[fieldType].getSimpleName();
            final String methodName = DATA_TYPE_TO_PRIMITIVE_SETTER.get(fieldType);
            final String methodDef = String.format("public void %s(IDataField dataField, %s _$_value)", methodName, valueType);

            generateFieldSwitchMethodBody(methodName, methodDef, ctClass, fields, true, valueType,
                    (dataField, localFieldName) -> {
                        return dataField.getFieldType() == fieldType
                                ? String.format("{ %1$s = false; this.%2$s = _$_value; } break;\n", getNullStateName(localFieldName), localFieldName)
                                : null; // Not our type, don't generate a case
                    });
        }
    }

    /**
     * Create the {@link IExtendedDataStructure#getField(IDataField)} method.
     *
     * @param ctClass The class being constructed
     * @param fields  The ordered list of structure fields to process
     * @since 2.31
     */
    private void createGetFieldMethod(CtClass ctClass, List<IDataField> fields) throws CannotCompileException
    {
        generateFieldSwitchMethodBody("getField", "public Object getField(IDataField dataField)",
                ctClass, fields, false, null,
                (dataField, localFieldName) -> dataField.isPrimitive()
                        ? String.format("return %1$s ? null : ($w) %2$s;\n", getNullStateName(localFieldName), localFieldName)
                        : "return " + localFieldName + ";\n");
    }

    /**
     * Create the {@link IExtendedDataStructure#setField(IDataField, Object)} method.
     *
     * @param ctClass The class being constructed
     * @param fields  The ordered list of structure fields to process
     * @since 2.31
     */
    private void createSetFieldMethod(CtClass ctClass, List<IDataField> fields) throws CannotCompileException
    {
        generateFieldSwitchMethodBody("setField", "public void setField(IDataField dataField, Object _$_value)",
                ctClass, fields, false, null,
                (dataField, localFieldName) -> {
                    if (dataField.isPrimitive())
                    {
                        return String.format("if (_$_value == null) %1$s = true; else { %1$s = false; %2$s = %3$s; }; break;\n",
                                getNullStateName(localFieldName), localFieldName, DATA_TYPE_TO_CAST_UNBOX.get(dataField.getFieldType()));
                    } else
                    {
                        final Class<?> javaType = dataField.getJavaType();

                        // Intern all string values
                        if (javaType == String.class)
                        {
                            return String.format("%1$s = (_$_value != null ? ((String) _$_value).intern() : null); break;\n", localFieldName);
                        } else
                        {
                            return String.format("%1$s = (%2$s) _$_value; break;\n", localFieldName, javaType.getCanonicalName());
                        }
                    }
                });
    }

    /**
     * Create the {@link IExtendedDataStructure#isNull(IDataField)} method.
     *
     * @param ctClass The class being constructed
     * @param fields  The ordered list of structure fields to process
     * @since 2.31
     */
    private void createIsNullMethod(CtClass ctClass, List<IDataField> fields) throws CannotCompileException
    {
        generateFieldSwitchMethodBody("isNull", "public boolean isNull(IDataField dataField)",
                ctClass, fields, false, null,
                (dataField, localFieldName) -> dataField.isPrimitive()
                        ? "return " + getNullStateName(localFieldName) + ";\n"
                        : "return " + localFieldName + " == null;\n");
    }

    /**
     * Generate the method body for a method that uses an input DataField to determine which internal field to
     * operate upon.  The main body consists of a switch statement and a case statement for each field name
     * (see {@link IDataField#getFieldName()}).  The caller provides a {@code caseGenerator} function to generate
     * the code for each case statement.
     *
     * @param methodName    Name of the method to create
     * @param methodDef     Method definition
     * @param ctClass       The class being constructed
     * @param fields        The ordered list of structure fields to process (note that the order of this list isn't
     *                      relevant here, we use the field's internal index to generate the switch entries)
     * @param reportType    Pass {@code true} if the default case should report a type mismatch, or {@code false} to report no such field
     * @param valueType     Value type to report if {@code reportType} is true
     * @param caseGenerator BiFunction to produce case block, if it returns {@code null}, no case is generated,
     *                      function arguments are the data field and the neutralized field name
     * @since 2.31
     */
    private void generateFieldSwitchMethodBody(String methodName, String methodDef, CtClass ctClass, List<IDataField> fields,
                                               boolean reportType, String valueType, BiFunction<IDataField, String, String> caseGenerator)
            throws CannotCompileException
    {
        final StringBuilder body = new StringBuilder();

        body.append(methodDef).append(" {\n");
        body.append("    switch(dataField.getNeutralizedFieldName()) {\n");

        for (IDataField dataField : fields)
        {
            final String line = caseGenerator.apply(dataField, dataField.getNeutralizedFieldName());
            if (line != null)
            {
                body.append("        case \"").append(dataField.getNeutralizedFieldName()).append("\": ").append(line);
            }
        }

        final String makeException = reportType
                ? "ClassCastException(dataField.getFieldName() + \" is not of type " + valueType + "\")"
                : "NoSuchFieldError(dataField.getFieldName())";

        body.append("        default: throw new ").append(makeException).append(";\n");
        body.append("    }\n}\n");

        if (logger.isDebugEnabled()) logger.debug("{}.{} body:\n{}", ctClass.getSimpleName(), methodName, body);

        ctClass.addMethod(CtNewMethod.make(body.toString(), ctClass));
    }

    /**
     * Create the constant field holding the SQL statement used to insert values into the extended data table.
     *
     * @param ctClass The class being constructed
     * @param fields  The list of fields (in order) to process
     * @since 2.14
     */
    private void createInsertSqlConstant(CtClass ctClass, List<IDataField> fields, String columns) throws NotFoundException, CannotCompileException
    {
        final String insertSqlValue = "INSERT INTO " + ctClass.getName() + " (" + columns + ") VALUES (" +
                "?,".repeat(fields.size()) +
                "?)"; // One more for the mappoint_id

        createStringConstant(ctClass, INSERT_SQL, CtField.Initializer.constant(insertSqlValue));
    }

    /**
     * Create the constant field holding the SQL statement used to insert values into the extended data table.
     *
     * @param ctClass The class being constructed
     * @since 2.14
     */
    private void createSelectSqlConstant(CtClass ctClass, String columns) throws NotFoundException, CannotCompileException
    {
        final String selectSqlValue = "SELECT " + columns + " FROM " + ctClass.getName();

        createStringConstant(ctClass, SELECT_SQL, CtField.Initializer.constant(selectSqlValue));
    }

    /**
     * Create the constant field holding the SQL statement used to insert values into the extended data table.
     *
     * @param ctClass The class being constructed
     * @since 2.14
     */
    private void createCopySqlConstant(CtClass ctClass, String columns) throws NotFoundException, CannotCompileException
    {
        final String copySqlValue = "COPY " + ctClass.getName() + " (" + columns + ")  FROM STDIN with delimiter '" + PostgresCopyUtils.COPY_DELIMITER + "'";

        createStringConstant(ctClass, COPY_SQL, CtField.Initializer.constant(copySqlValue));
    }

    /**
     * Create a static final String field in the class.
     *
     * @param ctClass     The class being constructed
     * @param fieldName   Name of field to create
     * @param initializer Initializer providing the constant value
     * @since 2.29
     */
    private void createStringConstant(CtClass ctClass, String fieldName, CtField.Initializer initializer) throws NotFoundException, CannotCompileException
    {
        final var field = new CtField(classPool.get(String.class.getCanonicalName()), fieldName, ctClass);
        field.setModifiers(CONSTANT_MODIFIERS);
        ctClass.addField(field, initializer);
    }

    /**
     * Create the method (named bindFieldValues) to bind the values in this data object into a prepared statement.
     * This method is the implementation for {@link IExtendedDataStructure#bindFieldValues(PreparedStatement, int)}.
     *
     * @param ctClass The class being constructed
     * @param fields  The fields (in order) to process
     * @since 2.14
     */
    private void createBindFieldValues(CtClass ctClass, List<IDataField> fields) throws CannotCompileException
    {
        final StringBuilder sb = new StringBuilder();

        // Args:
        // 1. null state variable name
        // 2. field index
        // 3. field SQL type
        // 4. sql setter method name
        // 5. field name
        //
        final String primitiveHandler =
                "if (%1$s) {%n" +
                        "    statement.setNull(%2$d, %3$d);%n" +
                        "} else {%n" +
                        "    statement.%4$s(%2$d, %5$s);%n" +
                        "}%n";

        // Args:
        // 1. field name
        // 2. field index
        // 3. field SQL type
        // 4. code to bind the value (call the setter on the statement)
        //
        final String referenceHandler =
                "if (null == %1$s) {%n" +
                        "    statement.setNull(%2$d, %3$d);%n" +
                        "} else {%n" +
                        "    %4$s;%n" +
                        "}%n";

        sb.append("public void bindFieldValues(java.sql.PreparedStatement statement, int rdmId) {\n");

        int fieldIndex = 1;
        for (IDataField field : fields)
        {
            final int fieldType = field.getFieldType();
            final String fieldName = field.getNeutralizedFieldName();
            final int sqlType = field.getSQLType();

            final String code;

            if (field.isPrimitive())
            {
                final String nullStateName = getNullStateName(fieldName);
                final String setterMethod = DATA_TYPE_TO_PRIMITIVE_SETTER.get(fieldType);

                code = String.format(primitiveHandler, nullStateName, fieldIndex, sqlType, setterMethod, fieldName);
            } else
            {
                final String setter = switch (fieldType)
                {
                    case IDataType.STRING -> "statement.setString(" + fieldIndex + ", " + fieldName + ")";

                    case IDataType.DATE -> "statement.setTimestamp(" + fieldIndex + ", new Timestamp(" + fieldName + ".getTime()))";

                    case IDataType.BYTE_ARRAY -> "statement.setBytes(" + fieldIndex + ", " + fieldName + ")";

                    case IDataType.STRING_ARRAY, IDataType.BOOLEAN_ARRAY, IDataType.INT_ARRAY, IDataType.FLOAT_ARRAY, IDataType.SHORT_ARRAY, IDataType.LONG_ARRAY, IDataType.DOUBLE_ARRAY, IDataType.DATE_ARRAY -> "statement.setBytes(" + fieldIndex + ", DatabaseUtils.getBytes(" + fieldName + "))";

                    default -> throw new IllegalStateException("Unhandled field type: " + fieldType);
                };

                code = String.format(referenceHandler, fieldName, fieldIndex, sqlType, setter);
            }

            sb.append(code);

            fieldIndex++;
        }

        // Add the map point ID as the final parameter
        sb.append("statement.setInt(").append(fieldIndex).append(',').append("rdmId").append(");\n");

        sb.append("}\n");

        final String body = sb.toString();
        logger.trace("   binder :\n{}", body);

        ctClass.addMethod(CtNewMethod.make(body, ctClass));
    }

    /**
     * Create the method (named buildCopyString) to create the string value to use for a row of data in a "COPY FROM".
     * This method is the implementation for {@link IExtendedDataStructure#bindFieldValues(PreparedStatement, int)}.
     *
     * @param ctClass The class being constructed
     * @param fields  The fields (in order) to process
     * @since 2.14
     */
    private void createBuildCopyString(CtClass ctClass, List<IDataField> fields) throws CannotCompileException
    {
        final StringBuilder code = new StringBuilder();

        // Args:
        // 1. null state variable name
        // 2. field name
        //
        final String primitiveHandler =
                "if (%1$s) {%n" +
                        "    sb.append(\"\\\\N\");%n" +
                        "} else {%n" +
                        "    sb.append(%2$s);%n" +
                        "}%n";

        // Args:
        // 1. field name
        // 2. code to build the field value
        //
        final String referenceHandler =
                "if (null == %1$s) {%n" +
                        "    sb.append(\"\\\\N\");%n" +
                        "} else {%n" +
                        "    %2$s;%n" +
                        "}%n";

        code.append("public void buildCopyString(StringBuilder sb, int rdmId) {\n");

        boolean firstField = true;

        for (IDataField field : fields)
        {
            if (firstField)
            {
                firstField = false;
            } else
            {
                code.append("sb.append(PostgresCopyUtils.COPY_DELIMITER);\n"); // Append the field separator
            }

            final int fieldType = field.getFieldType();
            final String fieldName = field.getNeutralizedFieldName();

            final String fieldCode;

            if (field.isPrimitive())
            {
                fieldCode = String.format(primitiveHandler, getNullStateName(fieldName), fieldName);
            } else
            {
                final String encoder = switch (fieldType)
                {
                    case IDataType.STRING -> "PostgresCopyUtils.encodeString(sb, " + fieldName + ")";

                    case IDataType.DATE -> "PostgresCopyUtils.encodeTimestamp(sb, " + fieldName + ")";

                    case IDataType.BYTE_ARRAY -> "PostgresCopyUtils.encodeBytes(sb, " + fieldName + ")";

                    case IDataType.STRING_ARRAY, IDataType.BOOLEAN_ARRAY, IDataType.INT_ARRAY, IDataType.FLOAT_ARRAY, IDataType.SHORT_ARRAY, IDataType.LONG_ARRAY, IDataType.DOUBLE_ARRAY, IDataType.DATE_ARRAY -> "PostgresCopyUtils.encodeBytes(sb, DatabaseUtils.getBytes(" + fieldName + "))";

                    default -> throw new IllegalStateException("Unhandled field type: " + fieldType);
                };

                fieldCode = String.format(referenceHandler, fieldName, encoder);
            }

            code.append(fieldCode);
        }

        // Add the map point ID as the final parameter
        if (!firstField)
        {
            code.append("sb.append(PostgresCopyUtils.COPY_DELIMITER);\n"); // Append the field separator
        }

        code.append("sb.append(rdmId).append('\\n');\n");

        code.append("}\n");

        final String body = code.toString();
        logger.trace("   copy :\n{}", body);

        ctClass.addMethod(CtNewMethod.make(body, ctClass));
    }

    /**
     * Create the method (named loadFieldValues) to load the values from a ResultSet into this data object.
     * This method is the implementation for {@link IExtendedDataStructure#loadFieldValues(ResultSet)}.
     *
     * @param ctClass The class being constructed
     * @param fields  The fields (in order) to process
     * @since 2.14
     */
    private void createLoadFieldValues(CtClass ctClass, List<IDataField> fields) throws CannotCompileException
    {
        final StringBuilder sb = new StringBuilder();

        // Args:
        // 1. field name
        // 2. sql getter method name
        // 3. null state variable name
        // 4. field index
        //
        final String primitiveHandler =
                "%1$s = results.%2$s(%4$d);%n" +
                        "%3$s = results.wasNull();%n";

        sb.append("public void loadFieldValues(java.sql.ResultSet results) {\n");
        sb.append("Timestamp timestamp;\n");
        sb.append("byte[] bytes;\n");

        int fieldIndex = 1;

        for (IDataField field : fields)
        {
            final int fieldType = field.getFieldType();
            final String fieldName = field.getNeutralizedFieldName();
            final String code;

            if (field.isPrimitive())
            {
                final String nullStateName = getNullStateName(fieldName);
                final String getterMethod = DATA_TYPE_TO_PRIMITIVE_GETTER.get(fieldType);

                code = String.format(primitiveHandler, fieldName, getterMethod, nullStateName, fieldIndex);
            } else
            {
                switch (fieldType)
                {
                    case IDataType.STRING:
                        code = fieldName + " = results.getString(" + fieldIndex + ");\n" +
                                "if (" + fieldName + " != null) " + fieldName + " = " + fieldName + ".intern();\n";
                        break;
                    case IDataType.DATE:
                        code = "timestamp = results.getTimestamp(" + fieldIndex + ");\n" +
                                fieldName + " = (timestamp == null) ? null : new java.util.Date(timestamp.getTime());\n";
                        break;
                    case IDataType.BYTE_ARRAY:
                        code = fieldName + " = results.getBytes(" + fieldIndex + ");\n";
                        break;
                    case IDataType.STRING_ARRAY:
                    case IDataType.BOOLEAN_ARRAY:
                    case IDataType.INT_ARRAY:
                    case IDataType.FLOAT_ARRAY:
                    case IDataType.SHORT_ARRAY:
                    case IDataType.LONG_ARRAY:
                    case IDataType.DOUBLE_ARRAY:
                    case IDataType.DATE_ARRAY:
                    {
                        final String arrayUtil = DATA_TYPE_TO_ARRAY_UTIL.get(fieldType);

                        // javassist's compiler is a little lame, so we need to add a cast so it knows 'null' is of the proper type
                        final String typeClassName = field.getJavaType().getCanonicalName();

                        code = "bytes = results.getBytes(" + fieldIndex + ");\n" +
                                fieldName + " = (bytes == null) ? (" + typeClassName + ") null : DatabaseUtils." + arrayUtil + "(bytes);\n";
                        break;
                    }
                    default:
                        throw new IllegalStateException("Unhandled field type: " + fieldType);
                }
            }

            sb.append(code);

            fieldIndex++;
        }

        sb.append("}\n");

        final String body = sb.toString();
        logger.trace("   loader :\n{}", body);

        ctClass.addMethod(CtNewMethod.make(body, ctClass));
    }

    /**
     * @param fieldName Name of field of interest
     * @return The name of the {@code null} state tracker member variable
     * @since 2.14
     */
    private String getNullStateName(String fieldName)
    {
        return "_isNull_" + fieldName;
    }

    /**
     * @param dataField Field of interest
     * @return The name of the "is null" test method for the given field, return type is boolean
     * @since 2.14
     */
    public static String getIsNullMethodName(IDataField dataField)
    {
        return "isNull_" + dataField.getNeutralizedFieldName();
    }

    /**
     * @param dataField Field of interest
     * @return The name of the method that returns the value of the field
     * @since 2.14
     */
    public static String getGetterMethodName(IDataField dataField)
    {
        return "get" + dataField.getNeutralizedFieldName();
    }

    /**
     * @param dataField Field of interest
     * @return The name of the method that sets the value of the field
     * @since 2.14
     */
    public static String getSetterMethodName(IDataField dataField)
    {
        return "set" + dataField.getNeutralizedFieldName();
    }

    /**
     * @param dataField Field of interest
     * @return The name of the "set null" method for the given field
     * @since 2.19
     */
    public static String getSetNullMethodName(IDataField dataField)
    {
        return "setNull_" + dataField.getNeutralizedFieldName();
    }

    /**
     * @param structure Structure of interest
     * @return The SQL statement to use to insert values into the DB backing table for the given structure
     * @since 2.14
     */
    public String getInsertSql(IStructure structure)
    {
        structure.createExtendedDataClass(); // Ensure that the dynamic class has been created

        final String className = structure.getExtendedDataClassName();
        final Field field = insertSqlFieldMap.get(className);
        try
        {
            return (String) field.get(null);
        } catch (IllegalAccessException e)
        {
            logger.error("Failed to get INSERT_SQL on {}", className, e);
            return null;
        }
    }

    /**
     * @param structure Structure of interest
     * @return The SQL statement to use to select values from the DB backing table for the given structure
     * @since 2.14
     */
    public String getSelectSql(IStructure structure)
    {
        structure.createExtendedDataClass(); // Ensure that the dynamic class has been created

        final String className = structure.getExtendedDataClassName();
        final Field field = selectSqlFieldMap.get(className);
        try
        {
            return (String) field.get(null);
        } catch (IllegalAccessException e)
        {
            logger.error("Failed to get SELECT_SQL on {}", className, e);
            return null;
        }
    }

    /**
     * @param structure Structure of interest
     * @return The SQL statement to use to COPY values into the structure table
     * @since 2.14
     */
    public String getCopySql(IStructure structure)
    {
        structure.createExtendedDataClass(); // Ensure that the dynamic class has been created

        final String className = structure.getExtendedDataClassName();
        final Field field = copySqlFieldMap.get(className);
        try
        {
            return (String) field.get(null);
        } catch (Exception e)
        {
            logger.error("Failed to get COPY_SQL on {}", className, e);
            return null;
        }
    }
}
