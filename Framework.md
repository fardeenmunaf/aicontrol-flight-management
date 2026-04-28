# EAPLI Framework (core, authz, pubsub)

This folder contains the compiled EAPLI framework modules used by LAPR4. Source files are not present, so the descriptions below are inferred from package and class names.

## Module: eapli.framework.core-25.0.0-RELEASE
Purpose: core building blocks used across the platform (DDD patterns, UI, utilities, infrastructure helpers).

### actions
Purpose: action abstractions, command/macro pattern, scheduling, and console menu models.
Classes:
- `Action` - executable action contract.
- `ActionHistoryKeeper` - tracks executed actions for undo/redo or auditing.
- `Actions` - helpers for creating or wrapping actions.
- `Actions$ThrowAction` - action wrapper that throws a runtime exception.
- `Actions$ThrowArgumentAction` - action wrapper that throws an illegal argument.
- `Actions$ThrowStateAction` - action wrapper that throws an illegal state.
- `ChainedAction` - runs multiple actions in sequence.
- `Command` - command pattern wrapper for a single action.
- `Macro` - composite action that groups actions.
- `TimedActions` - scheduler for time-based actions.
- `TimedActions$RecurringTask` - internal recurring task used by `TimedActions`.
- `UndoableAction` - action contract with undo support.
- `menu.Menu` - console menu container.
- `menu.MenuItem` - menu item model.
- `menu.MenuItem$ActionableMenuItem` - menu item that triggers an action.
- `menu.MenuItem$NonActionableMenuItem` - menu item used as label or separator.
- `menu.MenuItem$SubMenuItem` - menu item that opens a submenu.

### activerecord
Purpose: Active Record style persistence helpers.
Classes:
- `ActiveRecord` - base Active Record type with persistence operations.
- `DeleteableActiveRecord` - Active Record variant with delete support.
- `Finder` - query helper for Active Record lookups.
- `package-info` - package level documentation/annotations.

### application
Purpose: application layer base types.
Classes:
- `ApplicationService` - base class for application services.
- `UseCaseController` - controller base for use case execution.

### collections
Purpose: collection value objects and utilities.
Classes:
- `domain.model.ValueList` - value object wrapper around ordered lists.
- `domain.model.ValueSet` - value object wrapper around sets.
- `domain.model.HashValueSet` - hash-based implementation of `ValueSet`.
- `util.Arrays` - array utility helpers.
- `util.ArrayPredicates` - predicate helpers for arrays.
- `util.Collections` - collection utility helpers.

### csv
Purpose: CSV parsing and marshaling.
Classes:
- `CsvRecord` - representation of a CSV row.
- `CsvRecord$ParseState` - parser state for `CsvRecord`.
- `util.CsvLineMarshaler` - parse/serialize CSV lines.

### domain
Purpose: core DDD building blocks.
Classes:
- `events.DomainEvent` - domain event marker/interface.
- `events.DomainEventBase` - base class for domain events.
- `model.AggregateRoot` - aggregate root contract.
- `model.AggregateRootBase` - base class for aggregate roots.
- `model.DomainEntities` - helpers for domain entities.
- `model.DomainEntity` - entity contract.
- `model.DomainEntityBase` - base class for entities.
- `model.DomainFactory` - factory base for domain objects.
- `model.Immutable` - marker/annotation for immutable types.
- `model.ValueObject` - value object contract.
- `model.ValueObjectParser` - parse helpers for value objects.
- `model.package-info` - package level documentation/annotations.
- `repositories.BusinessIdentity` - business identity contract.
- `repositories.ConcurrencyException` - concurrency exception for repositories.
- `repositories.DomainRepository` - repository contract.
- `repositories.IntegrityViolationException` - integrity exception for repositories.
- `repositories.IterableDomainRepository` - iterable repository extension.
- `repositories.LockableDomainRepository` - repository with locking support.
- `repositories.ReportingRepository` - repository reporting extension.
- `repositories.TransactionalContext` - transactional context abstraction.
- `repositories.package-info` - package level documentation/annotations.
- `services.DomainService` - base class/marker for domain services.

### domains
Purpose: generic domain abstractions (intervals, discrete domains).
Classes:
- `domain.model.DiscreteDomain` - discrete domain definition.
- `domain.model.DiscreteDomainBuilder` - builder for discrete domains.
- `domain.model.Interval` - interval value object.
- `domain.model.Interval$BoundaryLimitType` - boundary limit enum for intervals.
- `domain.model.Interval$IntervalBuilder` - builder for intervals.

### functional
Purpose: functional programming helpers (Either, Try, stream utilities).
Classes:
- `ArrayOfDoubleCollector` - stream collector for double arrays.
- `ArrayOfDoubleCollector$1` - internal collector implementation.
- `Either` - sum type for left/right values.
- `Either$LeftProjection` - left view of `Either`.
- `Either$RightProjection` - right view of `Either`.
- `EmptyEither` - empty variant of `Either`.
- `EmptyLeft` - empty left value.
- `EmptyRight` - empty right value.
- `Functions` - functional helper methods.
- `LeftEither` - left-biased `Either` implementation.
- `MathFunctions` - math related function helpers.
- `Monad` - monad contract.
- `RightEither` - right-biased `Either` implementation.
- `StandardFunctions` - standard functional utilities.
- `Streams` - stream utility helpers.
- `Streams$1` - internal stream helper implementation.
- `Try` - computation wrapper for success/failure.
- `ValueHistory` - immutable history of values.
- `ValueHistory$HistoryIterator` - iterator for `ValueHistory`.

### general
Purpose: general-purpose value objects.
Classes:
- `domain.model.Description` - textual description value object.
- `domain.model.Designation` - designation/name value object.
- `domain.model.EmailAddress` - email value object with validation.
- `domain.model.Text` - text value object.
- `domain.model.Text$TextBuilder` - builder for `Text`.
- `domain.model.Text$WordCounterCollector` - word count collector for `Text`.

### identities
Purpose: identity and ID generation.
Classes:
- `Identifiable` - contract for identifiable domain objects.
- `IdentityGenerator` - identity generator contract.
- `IdentityGenerators` - registry/factory for identity generators.
- `IdentityGenerators$LazyHolder` - lazy singleton holder.
- `impl.HexSequenceGenerator` - hex sequence ID generator.
- `impl.NumberSequenceGenerator` - numeric sequence ID generator.
- `impl.PrefixedNumberSequenceGenerator` - prefixed numeric ID generator.
- `impl.UUIDGenerator` - UUID based ID generator.

### infrastructure
Purpose: infrastructure services and repository base implementations.
Classes:
- `InfrastructureService` - base for infrastructure services.
- `repositories.Repository` - base repository contract.
- `repositories.IterableRepository` - iterable repository extension.
- `repositories.LockableRepository` - repository locking extension.
- `repositories.package-info` - package level documentation/annotations.
- `repositories.impl.inmemory.InMemoryAutoNumberRepository` - in-memory repository with auto numbering.
- `repositories.impl.inmemory.InMemoryDomainAutoNumberRepository` - in-memory domain repo with auto numbering.
- `repositories.impl.inmemory.InMemoryDomainRepository` - in-memory domain repository base.
- `repositories.impl.inmemory.InMemoryRepository` - in-memory repository base.
- `repositories.impl.inmemory.InMemoryTransactionalContext` - in-memory transactional context.
- `repositories.impl.jpa.JpaAbstractRepository` - JPA repository base.
- `repositories.impl.jpa.JpaAutoTxRepository` - JPA repository with auto transactions.
- `repositories.impl.jpa.JpaBaseRepository` - base JPA repository.
- `repositories.impl.jpa.JpaBaseRepository$JpaPagedIterator` - paged iterator for JPA queries.
- `repositories.impl.jpa.JpaTransactionalContext` - JPA transactional context.
- `repositories.impl.jpa.JpaTransactionalRepository` - transactional JPA repository base.
- `repositories.impl.jpa.JpaWithTransactionalContextRepository` - JPA repository using external context.
- `repositories.impl.jpa.MoneyConverter` - JPA converter for `Money`.
- `repositories.impl.jpa.package-info` - package level documentation/annotations.
- `repositories.impl.springdata.SpringDataBaseRepository` - Spring Data repository base.
- `repositories.impl.springdata.SpringDataDomainBaseRepository` - Spring Data domain repo base.
- `repositories.impl.springdata.SpringDataLockableBaseRepository` - Spring Data base with locking.

### io
Purpose: IO utilities.
Classes:
- `StreamGobbler` - consumes process output streams.
- `util.Console` - console input/output helpers.
- `util.Files` - file utility helpers.

### math
Purpose: math utilities and value objects.
Classes:
- `domain.model.FrequencyTable` - frequency table value object.
- `domain.model.Matrix` - matrix value object.
- `domain.model.Numeral` - numeral value object.
- `domain.model.NumeralConverter` - numeral conversion logic.
- `domain.model.NumeralSystem` - numeral system contract.
- `domain.model.Percentage` - percentage value object.
- `domain.model.RomanNumeral` - roman numeral value object.
- `domain.model.StandardNumeralSystem` - standard numeral system.
- `domain.model.Vector` - vector value object.
- `domain.model.Vector$VectorType` - vector type enum.
- `util.Math` - math utility helpers.
- `util.NumberGenerator` - number generator helpers.
- `util.NumberPredicates` - predicates for numbers.

### memento
Purpose: memento pattern support.
Classes:
- `Memento` - snapshot representation.
- `Restorable` - contract for restorable objects.
- `RestorableHistoryKeeper` - history manager for `Restorable`.

### money
Purpose: money and financial value objects.
Classes:
- `domain.model.CashFlow` - cash flow value object.
- `domain.model.CashFlowSeries` - series of cash flows.
- `domain.model.ConversionRate` - currency conversion rate.
- `domain.model.ForeignExchange` - foreign exchange service/model.
- `domain.model.Money` - money value object.
- `domain.model.Money$MoneyCollector` - collector for `Money` values.

### presentation
Purpose: console UI building blocks.
Classes:
- `console.AbstractListUI` - base class for list UIs.
- `console.AbstractUI` - base class for console UIs.
- `console.EnumSelectWidget` - widget for selecting enum values.
- `console.ExitWithMessageAction` - action to exit UI with message.
- `console.ListUI` - list UI implementation.
- `console.ListWidget` - widget for listing items.
- `console.SelectWidget` - generic selection widget.
- `console.ShowMessageAction` - action to show a message.
- `console.ShowUiAction` - action to show another UI.
- `console.package-info` - package level documentation/annotations.
- `console.menu.HorizontalMenuRenderer` - renderer for horizontal menus.
- `console.menu.MenuItemRenderer` - renderer for menu items.
- `console.menu.MenuRenderer` - menu renderer contract/base.
- `console.menu.VerticalMenuRenderer` - renderer for vertical menus.

### quantities
Purpose: quantities and units.
Classes:
- `domain.model.Quantifiable` - contract for quantifiable values.
- `domain.model.StandardUnits` - standard unit definitions.
- `domain.model.Temperature` - temperature value object.
- `domain.model.Temperature$TemperatureCollector` - collector for temperatures.
- `domain.model.Temperature$TemperatureUnit` - temperature unit enum.
- `domain.model.Temperature$TemperatureUnit$1` - internal enum constant.
- `domain.model.Temperature$TemperatureUnit$2` - internal enum constant.
- `domain.model.Temperature$TemperatureUnit$3` - internal enum constant.
- `domain.model.Unit` - unit value object.

### representations
Purpose: DTO and representation builders.
Classes:
- `RepresentationBuilder` - base for representation builders.
- `Representationable` - contract for representable objects.
- `builders.GeneralDTORepresentationBuilder` - builder for generic DTOs.
- `builders.JsonRepresentationBuilder` - JSON builder for representations.
- `builders.JsonRepresentationBuilderState` - builder state holder.
- `builders.JsonRepresentationBuilderState$JsonDocumentType` - JSON document type enum.
- `builders.JsonRepresentationBuilderState$StateContext` - state context for builder.
- `builders.JsonRepresentationBuilderState$StateContext$StateContextType` - state context type.
- `dto.DTO` - DTO contract/base.
- `dto.DTOParser` - parser for DTOs.
- `dto.DTOable` - contract for objects that can be converted to DTOs.
- `dto.GeneralDTO` - generic DTO implementation.

### services
Purpose: small service utilities.
Classes:
- `ResourceNotFoundException` - exception for missing resources.
- `RestConsierge` - REST helper/concierge for remote resources.

### strings
Purpose: string formatting and transformation utilities.
Classes:
- `BaseFormattedString` - base formatted string type.
- `CachedFormattedString` - formatted string with caching.
- `FormatingOrTransformationException` - exception for formatting errors.
- `FormattedMoney` - formatted money string.
- `FormattedString` - formatted string wrapper.
- `PrettyJsonString` - pretty printed JSON wrapper.
- `PrettyXmlString` - pretty printed XML wrapper.
- `PrettyXmlString$1` - internal transformer.
- `PrettyXmlString$2` - internal transformer.
- `RandomString` - random string generator.
- `StringMixin` - string mixin helpers.
- `XmlCurrencyAdapter` - XML adapter for currency.
- `XmlDateAdapter` - XML adapter for dates.
- `util.StringPredicates` - predicates for strings.
- `util.Strings` - string utility helpers.

### time
Purpose: time and temporal series helpers.
Classes:
- `domain.model.DateInterval` - date interval value object.
- `domain.model.TemporalEvent` - temporal event value object.
- `domain.model.TemporalEvent$1` - internal temporal event helper.
- `domain.model.TemporalSeries` - time series base.
- `domain.model.TimeInterval` - time interval value object.
- `domain.model.TimestampInterval` - timestamp interval value object.
- `domain.model.TreeTemporalSeries` - tree-based temporal series.
- `domain.model.TreeTemporalSeriesBuilder` - builder for tree temporal series.
- `util.Calendars` - calendar helper utilities.
- `util.CalendarPredicates` - predicates for calendars.
- `util.CurrentTimeCalendars` - helpers for current time calendars.
- `util.CurrentTimeCalendarPredicates` - predicates based on current time.

### util
Purpose: general utility patterns.
Classes:
- `Factory` - factory pattern helper.
- `HashCoder` - hash code helper.
- `Singleton` - singleton pattern helper.
- `Strategy` - strategy pattern contract.
- `TemplateMethod` - template method helper.
- `Utility` - base utility class marker.

### validations
Purpose: validation helpers and preconditions.
Classes:
- `Check` - fluent validation helper.
- `Check$FailedTry` - validation failure state.
- `Check$FollowupTry` - follow-up validation state.
- `CheckIf` - conditional validation helper.
- `ComparablePredicates` - predicates for comparables.
- `Invariants` - invariants validation helper.
- `Preconditions` - precondition validation helper.
- `Validations` - general validation helpers.

### visitor
Purpose: visitor pattern support.
Classes:
- `Visitor` - visitor contract.
- `Visitable` - visitable contract.

## Module: eapli.framework.infrastructure.authz-25.0.0-RELEASE
Purpose: authentication and authorization infrastructure (users, roles, passwords, repositories).

### application
Purpose: authentication and authorization application services.
Classes:
- `AuthzRegistry` - registry for authz services and dependencies.
- `AuthenticationService` - service for authenticating users.
- `Authenticator` - credential validation contract/adapter.
- `AuthorizationService` - service for role/permission checks.
- `UserDetailsServiceImpl` - user details adapter for external security frameworks.
- `UserManagementService` - user management use cases.
- `UserSession` - authenticated user session holder.
- `package-info` - package level documentation/annotations.

### application.exceptions
Purpose: auth specific exceptions.
Classes:
- `UnauthenticatedException` - thrown when no user is authenticated.
- `UnauthorizedException` - thrown when authorization fails.

### domain.model
Purpose: auth domain model (users, roles, password policies).
Classes:
- `Name` - person name value object.
- `NilPasswordPolicy` - no-op password policy.
- `Password` - password value object.
- `PasswordPolicy` - password policy contract.
- `PlainTextEncoder` - plaintext password encoder (test/dev use).
- `RandomRawPassword` - generator for random raw passwords.
- `Role` - role value object.
- `RoleAssignment` - role assignment to a user.
- `RoleSet` - collection of roles.
- `SystemUser` - system user entity.
- `SystemUserBuilder` - builder for system users.
- `SystemUserDetails` - read-only user details view.
- `Username` - username value object.
- `package-info` - package level documentation/annotations.

### domain.repositories
Purpose: repository contracts for auth domain.
Classes:
- `UserRepository` - repository for `SystemUser`.

### repositories.impl.inmemory
Purpose: in-memory repository implementations.
Classes:
- `InMemoryUserRepository` - in-memory user repository.

### repositories.impl.jpa
Purpose: JPA repository implementations.
Classes:
- `JpaAutoTxUserRepository` - JPA user repository with auto transactions.
- `JpaUserRepository` - JPA user repository implementation.
- `UserRepositoryConstants` - JPA query/field constants.

### repositories.impl.springdata
Purpose: Spring Data repository implementations.
Classes:
- `SpringDataUserRepository` - Spring Data user repository implementation.

### package-info
Purpose: root package metadata.
Classes:
- `package-info` - package level documentation/annotations.

## Module: eapli.framework.infrastructure.pubsub-25.0.0-RELEASE
Purpose: publish/subscribe infrastructure with in-process and simple persistent implementations.

### infrastructure.pubsub
Purpose: pub/sub core contracts and registry.
Classes:
- `EventDispatcher` - dispatches events to handlers.
- `EventHandler` - handler contract for events.
- `EventPublisher` - publishes events to a dispatcher/bus.
- `PubSubRegistry` - registry for selecting pub/sub implementation.
- `package-info` - package level documentation/annotations.

### impl.inprocess.service
Purpose: in-process pub/sub implementation.
Classes:
- `InProcessPubSub` - in-process pub/sub service.
- `InProcessPubSub$LazyHolder` - lazy singleton holder.
- `InProcessPublisherDispatcher` - dispatcher for in-process publishing.
- `package-info` - package level documentation/annotations.

### impl.simplepersistent
Purpose: persistent pub/sub implementation with storage-backed event records.
Classes:
- `package-info` - package level documentation/annotations.

### impl.simplepersistent.model
Purpose: persistence model for events and consumption.
Classes:
- `EventConsumption` - consumption state for an event by a subscriber.
- `EventRecord` - persistent event record.

### impl.simplepersistent.repositories
Purpose: repository contracts for persistence model.
Classes:
- `EventConsumptionRepository` - repository for event consumption.
- `EventRecordRepository` - repository for event records.

### impl.simplepersistent.repositories.jpa
Purpose: JPA repository implementations for persistent pub/sub.
Classes:
- `JpaAutoTxEventConsumptionRepository` - JPA repo with auto transactions.
- `JpaAutoTxEventRecordRepository` - JPA repo with auto transactions.
- `JpaEventConsumptionRepository` - JPA event consumption repository.
- `JpaEventRecordRepository` - JPA event record repository.

### impl.simplepersistent.repositories.springdata
Purpose: Spring Data repository implementations for persistent pub/sub.
Classes:
- `SpringDataEventConsumptionRepository` - Spring Data event consumption repository.
- `SpringDataEventRecordRepository` - Spring Data event record repository.

### impl.simplepersistent.service
Purpose: services for persistent pub/sub operations.
Classes:
- `SimplePersistentPubSub` - persistent pub/sub service.
- `SimplePersistentPubSub$PubSubWrapper` - internal wrapper for pub/sub operations.
- `SimplePersistentPublisherDispatcher` - dispatcher for persistent publishing.
