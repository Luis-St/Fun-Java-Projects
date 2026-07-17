## Must-Have (Core Structural)

**Immutability & Copying**
- `@WithMethods`
- `@CopyConstructor`
- `@DeepCopy`
- `@AsUnmodifiable`

**Delegation & Forwarding**
- `@Delegate`
- `@Forward`

**Builders & Construction**
- `@Builder`
- `@FactoryMethod`

**Null Handling (Explicit)**
- `@NonNull`
- `@AddNullCheck`
- `@NullSafe`

**Basic Equality**
- `@ValueEquals`
- `@IdentityEquals`

---

## Should-Have (Common Patterns)

**Optional & Nullable Handling**
- `@UnwrapOptional`
- `@NullableReturn`
- `@OptionalGetter`

**Collection Helpers**
- `@CollectionHelpers`
- `@AddMethod`
- `@RemoveMethod`
- `@DefensiveCopy`

**Lenses & Updates**
- `@Lenses`
- `@UpdateMethod`

**Pattern Matching**
- `@SealedWithMatchers`
- `@MatchMethod`

**Fluent APIs**
- `@FluentSetter`
- `@Chainable`
- `@FluentBuilder`

**Conversion & Mapping**
- `@ConvertTo`
- `@MapTo`
- `@AsRecord`

**Validation (Explicit)**
- `@ValidateMethod`
- `@RequireValid`
- `@CheckInvariant`

---

## Could-Have (Advanced Structural)

**Lazy & Caching**
- `@Lazy`
- `@Memoize`
- `@CachedProperty`
- `@ComputeOnce`

**Resource Management**
- `@AutoClose`
- `@ManagedResource`
- `@CloseOrder`

**Decorator & Proxy Patterns**
- `@Decorator`
- `@Proxy`
- `@InterceptMethod`

**State Management**
- `@StateMachine`
- `@Transition`
- `@StateValidator`

**Error Handling**
- `@RetryOnException`
- `@FallbackTo`
- `@Suppress`
- `@WrapException`

**Serialization Control**
- `@SerializationProxy`
- `@CustomSerialization`
- `@TransientField`

**Threading & Synchronization**
- `@ThreadSafe`
- `@Synchronized`
- `@Volatile`
- `@Atomic`

**Logging & Monitoring**
- `@LogCalls`
- `@TimeExecution`
- `@TraceMethod`

**Composition Helpers**
- `@Mixin`
- `@Compose`
- `@Aggregate`

**Range & Bounds**
- `@Clamp`
- `@Range`
- `@BoundedValue`

**Formatting & String**
- `@ToString`
- `@FormatWith`
- `@JsonSerialize`

**Comparison & Ordering**
- `@Comparable`
- `@CompareTo`
- `@SortBy`

**Clone & Copy**
- `@DeepClone`
- `@ShallowClone`
- `@CloneStrategy`

**Access Control**
- `@ReadOnly`
- `@WriteOnce`
- `@ImmutableAfterConstruction`

**Defaults & Initialization**
- `@DefaultValue`
- `@InitializedBy`
- `@RequireInitialization`

**Conditional Logic**
- `@ConditionalMethod`
- `@WhenNull`
- `@WhenPresent`

**Builder Enhancements**
- `@BuilderDefault`
- `@BuilderValidation`
- `@RequiredField`

**Type Conversion**
- `@AutoConvert`
- `@ParseFrom`
- `@SerializeTo`

**Event & Observer**
- `@Observable`
- `@Publisher`
- `@Subscribe`

**Specialized Getters**
- `@LazyGetter`
- `@ComputedGetter`
- `@CachedGetter`

**Method Generation**
- `@GenerateEquals`
- `@GenerateHashCode`
- `@GenerateCompareTo`

**Wrapper Patterns**
- `@Wrapper`
- `@Unwrap`
- `@DelegateAll`
