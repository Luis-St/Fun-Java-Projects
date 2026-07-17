# External Code Generation Configuration
## Idea
The idea of the ecgc is to provide a centralized configuration for code generation.
This configuration can be used to enable or disable specific code generation features across an entire project, module, package, class, or even individual fields and methods.
This allows developers to have fine-grained control over which code generation features are applied in different parts of their codebase.

## Configuration Levels
The ecgc can be applied at various levels of granularity:
- **Project Level**: Global settings that apply to the entire project.
- **Module Level**: Settings that apply to specific modules within the project.
- **Package Level**: Settings that apply to all classes within a specific package.
- **Class Level**: Settings that apply to a specific class.
- **Field/Method Level**: Settings that apply to specific fields or methods within a class.

## Configuration Options
The ecgc can include options to enable or disable specific code generation features.
The configuration can also predefine parameters for code generation to ensure consistency across the codebase.
It is also possible to completely configure the code generation behavior to skip the configuration via annotations and only rely on the ecgc.

## Code Generation
The code generation is based on transformers that read the code and generate additional code based on the configuration.
These transformers can be configured to apply specific code generation features based on the ecgc settings.
All transformers can be enabled or disabled via the ecgc.
The detailed configuration of each transformer is based on its own configuration options.
The configuration options are inferred from the constructor parameters of each transformer.

### Inferred Configuration Options
Each transformer is implemented as a class with a constructor that takes specific parameters.
The class name is always suffixed with "Transformer", the configuration option name is the class name without the "Transformer" suffix.
The constructor parameters define the configuration options for the transformer.
For example, a transformer class named `ExampleTransformer` with the following constructor:
```java
// Implemented as a record for simplicity
public record ExampleTransformer(boolean enabled, String option1, int option2) {}
```
Would have the following configuration options:
- `enabled`: A boolean option to enable or disable the transformer (inherited from the base transformer class).
- `option1`: A string option for additional configuration.
- `option2`: An integer option for additional configuration.

Allowed parameter types are:
- `boolean`
- `byte`, `short`, `int`, `long`
- `float`, `double`
- `String`
- `Enum` types
- `List<T>` where T is one of the listed types
- `Map<String, T>` where T is one of the listed types
- `Set<T>` where T is one of the listed types
- `Optional<T>` where T is one of the listed types
- Record types composed of the listed types

## Transformer
A transformer can modify a java file by adding, removing, or modifying code elements.
The base transformer class provides the `enabled` option to enable or disable the transformer.
It also provides the following methods to be overridden by subclasses:
- `transformClass(ClassDeclaration classDecl)`: Transforms a class declaration
- `transformRecordComponent(RecordComponentDeclaration recordCompDecl)`: Transforms a record component declaration
- `transformField(FieldDeclaration fieldDecl)`: Transforms a field declaration
- `transformMethod(MethodDeclaration methodDecl)`: Transforms a method declaration

The transformer can then implement the desired code generation logic in these methods.
The transform methods are only called if the transformer is enabled and if the target matches the configuration.

## Transformer Management
Transformers are managed by a central transformer manager that reads the ecgc and applies the transformers based on the configuration.
The transformer manager is responsible for loading the transformers, reading the ecgc, and applying the transformers to the codebase.
It ensures that the correct transformers are applied based on the configuration levels and targets.

### Configuration Application
The configurations are applied in declaring order, where:
1. later configurations can override earlier ones with the same level and target
2. more specific levels override less specific ones (e.g., class level overrides package level)

If there are multiple configurations that apply to the same target at the same level, the last one declared takes precedence.

### Transformer Execution
When a matching configuration is found for a target, the transformers that are defined in the configuration are executed in the descending order of their declaration in the configuration.
This allows for predictable and consistent code generation behavior.
But this also means that the order of transformers in the configuration can affect the final generated code.
For example, if one transformer generates code that is then modified by another transformer, the order of execution matters.

#### Example
If we have:
1. a NotNullTransformer that adds null checks to parameters of methods,
2. a LoggingTransformer that adds logging statements to methods,
3. a Transformer that further validates the value (like checking ranges or formats)

And we apply them in the following order:
- ValidationTransformer
- LoggingTransformer
- NotNullTransformer

#### Why in descending order?
The reason for this is that each of the above listed transformers modifies the method body, and adds code at the start of the method.
If we would apply them in the order listed above, the transformers would be called like this:
1. NotNullTransformer adds null checks at the start of the method
2. LoggingTransformer adds logging statements at the start of the method (before the null checks)
3. ValidationTransformer adds validation code at the start of the method (before the logging and null checks)

This would result in the null checks being executed last, which is not the desired behavior.
Resulting in the following method body structure:
```java
void exampleMethod(String param) {
    // Validation code
    if (param does not meet criteria) {
        throw new IllegalArgumentException("Invalid parameter");
    }
    
    // Logging statement
    System.out.println("exampleMethod called with param: " + param);
    
    // Null check
    if (param == null) {
        throw new NullPointerException("param cannot be null");
    }
    
    // Original method body
}
```

By applying the transformers in descending order, we ensure that the null checks are executed first, followed by the logging statements, and finally the validation code.
This results in the following method body structure:
```java
void exampleMethod(String param) {
	// Null check
    if (param == null) {
		throw new NullPointerException("param cannot be null");
	}
	
	// Logging statement
    System.out.println("exampleMethod called with param: " + param);
    
    // Validation code
    if (param does not meet criteria) {
        throw new IllegalArgumentException("Invalid parameter");
    }
    
    // Original method body
}
```

## Configuration Syntax
```
config <name> {
    // Specifies the level at which this configuration applies:
    // project, module, package, class, field, method
    level <level>!; 
    // Specifies the target(s) for this configuration
    // It specifies what this configuration covers
    target {
        // Specifies the target type:
        // class, field, method, record-component
        type <target-type>!;
        
        // ToDo:
        //  Improve parent.generics
        //  Add constraints.generics
        //  Improve constraints.parameters
        
        // Only for field, method, record-component targets:
        parent {
            signature ("<target-signature>"|Wildcard("<target-signature>")|Regex("<target-signature-regex>"))?;
            
            access (<public|protected|private|package-private>)?;
            modifiers final && static && !abstract && ... ;
            name ("<parent-name>"|Wildcard("<parent-name>")|Regex("<parent-name-regex>"))?;
            generics ("<generic>"|Wildcard("<generic>")|Regex("<generic-regex>"))?; // Supports expressions with &&, ||, !
            extends ("<super-class>"|Wildcard("<super-class>")|Regex("<super-class-regex>"))?; // Supports expressions with &&, ||, !
            implements ("<interface>"|Wildcard("<interface>")|Regex("<interface-regex>"))?; // Supports expressions with &&, ||, !
        }
        
        constraints {
            // All:
            access (<public|protected|private|package-private>)?;
            modifiers final && static && abstract && ... ;
            name ("<target-name>"|Wildcard("<target-name>")|Regex("<target-name-regex>"))?;
            annotations ("<annotation>"|Wildcard("<annotation>")|Regex("<annotation-regex>"))?; // Supports expressions with &&, ||, ! (only name validation, no parameters)
            
            // Classes & Records:
            extends ("<super-class>"|Wildcard("<super-class>")|Regex("<super-class-regex>"))?; // Supports expressions with &&, ||, !
            implements ("<interface>"|Wildcard("<interface>")|Regex("<interface-regex>"))?; // Supports expressions with &&, ||, !
            
            // Fields:
            type ("<field-type>"|Wildcard("<field-type>")|Regex("<field-type-regex>"))?; // Supports expressions with &&, ||, !
            
            // Methods:
            return-type ("<return-type>"|Wildcard("<return-type>")|Regex("<return-type-regex>"))?; // Supports expressions with &&, ||, !
            parameters [("<param-type>"|Wildcard("<param-type>")|Regex("<param-type-regex>")), ...]; // Each parameter supports expressions with &&, ||, !
        }
    }
    
    settings {
        <transformer-name> {
            enabled: <true|false>;
            // Additional parameters specific to the transformer inferred from its constructor:
            // For primitive types: boolean, byte, short, int, long, float, double
            <parameter-name>: <value>;
            // For String type:
            <parameter-name>: "<string-value>";
            // For Enum types:
            <parameter-name>: <ENUM_CONSTANT>;
            // For List<T> type:
            <parameter-name>: [<value1>, <value2>, ...]; // Multi-line allowed
            // For Map<String, T> type:
            <parameter-name>: { "<key1>": <value1>, "<key2>: <value2>, ... }; // Multi-line allowed
            // For Set<T> type:
            <parameter-name>: (<value1>, <value2>, ... ); // Multi-line allowed
            // For Optional<T> type:
            <parameter-name>: <value>|None; // Use 'None' to represent an empty, absent value or obmit the parameter entirely
            // For Record types:
            <parameter-name>: {
                <field1>: <value1>;
                <field2>: <value2>;
                ...
            };
        }
        ...
    }
}
```
