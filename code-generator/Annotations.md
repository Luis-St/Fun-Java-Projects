# @WithMethods
## Parameters
- TypeAccess access: Specifies the access level (e.g., PUBLIC, PROTECTED, PACKAGE_PRIVATE, PRIVATE) for the generated 'with' methods. Default is PUBLIC.
  For record components, the access level is always PUBLIC.
## Description
Generates 'with' methods for all fields, allowing for immutable updates.
Each 'with' method returns a new instance with the specified field updated.
Requires a constructor that initializes all fields.

# @With
## Parameters
- String name: The name of the field or record component that is used to generate the 'with' method.
- TypeAccess access: Specifies the access level (e.g., PUBLIC, PROTECTED, PACKAGE_PRIVATE, PRIVATE) for the generated 'with' method. Default is PUBLIC.
  For record components, the access level is always PUBLIC.
## Description
Generates a 'with' method for the annotated field or record component, allowing for immutable updates.
The 'with' method returns a new instance with the specified field updated.
Requires a constructor that initializes all fields.

# @CopyConstructor
## Parameters
- TypeAccess access: Specifies the access level (e.g., PUBLIC, PROTECTED, PACKAGE_PRIVATE, PRIVATE) for the generated copy constructor. Default is PUBLIC.
- String[] exclude: An array of field names to exclude from the copy constructor.
## Description
Generates a copy constructor that creates a new instance by copying all fields from another instance of the same class.
Requires a constructor that initializes all non-excluded fields.

# @Delegate
## Parameters
- TypeAccess access: Specifies the access level (e.g., PUBLIC, PROTECTED, PACKAGE_PRIVATE, PRIVATE) for the generated delegate methods. Default is PUBLIC.
- String[] exclude: An array of simple method signatures to exclude from delegation (e.g., "toString", "createItem(String)"). Default is toString, hashCode, and equals.
## Description
Generates delegate methods for all methods of the annotated field's type.
The generated methods forward calls to the corresponding methods of the annotated field.

# @Forward
## Parameters
- TypeAccess access: Specifies the access level (e.g., PUBLIC, PROTECTED, PACKAGE_PRIVATE, PRIVATE) for the generated forward methods. Default is PUBLIC.
- String[] include: An array of simple method signatures to forward (e.g., "toString", "createItem(String)").
## Description
Generates forward methods for the specified methods of the annotated field's type.
The generated methods forward calls to the corresponding methods of the annotated field.
Similar to @Delegate, but only for explicitly listed methods.

# @Builder
## Parameters
- TypeAccess access: Specifies the access level (e.g., PUBLIC, PROTECTED, PACKAGE_PRIVATE, PRIVATE) for the generated builder class and methods. Default is PUBLIC.
- String className: The name of the generated builder class. Default is "Builder".
- boolean fluentSetters: If true, generates fluent setter methods in the builder. Default is true. (e.g., `builder.setField(value)` returns the builder instance)
- String methodName: The name of the build method in the builder class. Default is "build".
- String[] exclude: An array of field names to exclude from the builder.
## Description
Generates a builder class for the annotated class, allowing for fluent construction of instances.
The builder class contains setter methods for each field and a build method to create an instance of the annotated class.
Requires a constructor that initializes all non-excluded fields.

# @FactoryMethod
## Parameters
- TypeAccess access: Specifies the access level (e.g., PUBLIC, PROTECTED, PACKAGE_PRIVATE, PRIVATE) for the generated factory method. Default is PUBLIC.
- String methodName: The name of the generated factory method. Default is "of".
- String[] exclude: An array of field names to exclude from the factory method.
## Description
Generates a static factory method for the annotated class, allowing for convenient creation of instances.
The factory method takes parameters for each field and returns a new instance of the annotated class.
There must be a constructor that initializes all non-excluded fields.

# @UnwrapOptional
## Parameters
- TypeAccess access: Specifies the access level (e.g., PUBLIC, PROTECTED, PACKAGE_PRIVATE, PRIVATE) for the generated method. Default is PUBLIC.
- String baseName: The base name for the generated method. Default is "get" + capitalized field name.
- boolean generateOrThrow: If true, generates baseName + "OrThrow" method that throws NoSuchElementException if the Optional is empty. Default is true.
- boolean generateOrElse: If true, generates baseName + "OrElse" method that takes a default value parameter. Default is true.
- boolean generateOrElseGet: If true, generates baseName + "OrElseGet" method that takes a Supplier parameter. Default is true.
## Description
Generates helper methods for fields of type Optional<T> to unwrap the value.
The generated methods provide convenient ways to access the value inside the Optional, with options for default values and exception throwing.

# @Equals
## Parameters
- String[] exclude: An array of field names to exclude from the equals comparison.
## Description
Generates equals() method for all final fields of the class.
Can not be applied to records.

# @HashCode
## Parameters
- String[] exclude: An array of field names to exclude from the hashCode calculation.
## Description
Generates hashCode() method for all final fields of the class.
Can not be applied to records.

# @ToString
## Parameters
- String prefix: A prefix to add at the beginning of the toString output. Default is  "${this.getClass().getSimpleName()}{".
- String suffix: A suffix to add at the end of the toString output. Default is "}".
- String separator: A separator to use between fields in the toString output. Default is ", ".
- String[] exclude: An array of field names to exclude from the toString output.
## Description
Generates toString() method for all fields of the class.
Can not be applied to records.

# @ValueEquals
## Parameters
- String[] fields: An array of field names to include in the value-based equality comparison.
## Description
Generates equals() and hashCode() methods based on the specified fields for value-based equality.
Can be applied to both classes and records.

# @IdentityEquals
## Parameters
- None
## Description
Generates equals() and hashCode() methods based on object identity (i.e., using '==').
Can be applied to both classes and records.

# @CollectionHelpers
## Parameters
- TypeAccess access: Specifies the access level (e.g., PUBLIC, PROTECTED, PACKAGE_PRIVATE, PRIVATE) for the generated helper methods. Default is PUBLIC.
- String baseName: The base name for the generated methods. Default is the field name.
- boolean generateGet: If true, generates accessor methods for the collection field. Default is true.
- boolean generateContains: If true, generates methods to check for item existence in the collection field. Default is true.
- boolean generateAdd: If true, generates mutator methods to add items to the collection field. Default is true.
- boolean generateRemove: If true, generates mutator methods to remove items from the collection field. Default is true.
- boolean generateClear: If true, generates a method to clear all items from the collection field. Default is true.
## Description
Generates helper methods for collection fields (e.g., List, Set, Map) to facilitate common operations.
The generated methods include accessors (e.g., getItem, containsItem) and mutators (e.g., addItem, removeItem).


