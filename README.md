# Hibernate RX

A reactive API for Hibernate ORM, supporting non-blocking database
drivers and a reactive style of interaction with the database.

Hibernate RX may be used in any plain Java program, but is especially
targeted toward usage in reactive environments like 
[Quarkus](https://quarkus.io/) and [Vert.x](https://vertx.io/).

_This project is still at an experimental stage._

## Example program

There is a very simple example program in the `example` directory.

## Building

To compile, navigate to this directory, and type:

	./gradlew build

To publish Hibernate RX to your local Maven repository, run:

	./gradlew publishToMavenLocal

## Running tests

To run the tests, ensure that PostgreSQL is installed on your machine.
From the command line, type the following commands:

	psql
	create database "hibernate-rx";
	create user "hibernate-rx" with password 'hibernate-rx';
	grant all privileges on database "hibernate-rx" to "hibernate-rx";

Finally, run `./gradlew test` from this directory.

## Dependencies

The project has been tested with

- Java 8
- PostgreSQL
- [Hibernate ORM](https://hibernate.org/orm/) 5.4.10.Final
- [Vert.x Reactive PostgreSQL Client](https://vertx.io/docs/vertx-pg-client/java/) 0.0.12

## Usage

Usage is very straightforward for anyone with any prior experience with
Hibernate or JPA. 

#### Including Hibernate RX in your project

Add the following dependency to your project:

	org.hibernate.rx:hibernate-rx-core:1.0.0-SNAPSHOT

You'll also need to add Hibernate 5.4, the Vert.x reactive database
driver, and a regular JDBC driver (which is used for schema export).

There is an example Gradle build included in the example program.

#### Mapping entity classes

Use the regular JPA mapping annotations defined in the package 
`javax.persistence`, and/or the Hibernate mapping annotations in
`org.hibernate.annotations`.

Most mapping annotations are already supported in Hibernate RX. The
annotations which are not yet supported are listed in _Limitations_,
below.

#### Configuration

Configuration is completely transparent; configure Hibernate 
exactly as you normally would, for example by providing a
`META-INF/persistence.xml` file.

An example `persistence.xml` file is included in the example program.

#### Obtaining a reactive session factory

Obtain a Hibernate `SessionFactory` or JPA `EntityManagerFactory` 
just as you normally would, for example, by calling:

	EntityManagerFactory emf = createEntityManagerFactory("example");

 Now, `unwrap()` the `RxSessionFactory`:
 
	RxSessionFactory sessionFactory = emf.unwrap(RxSessionFactory.class);

#### Obtaining a reactive session

To obtain an `RxSession` from the `RxSessionFactory`, use `openRxSession()`:

	RxSession session = sessionFactory.openRxSession();

#### Using the reactive session

The `RxSession` interface has methods with the same names as methods of the
JPA `EntityManager`. However, each of these methods returns its result via
a `CompletionStage`, for example:

	session1.persist(book)
		.thenCompose( $ -> session1.flush() )
		.thenAccept( $ -> session1.close() )

Note that `find()` also wraps its result in `Optional`:

	session2.find(Book.class, book.id)
		.thenApply(Optional::get)
		.thenAccept( bOOk -> System.out.println(bOOk.title + " is a great book!") )
		.thenAccept( $ -> session2.close() )

## Limitations

At this time, Hibernate RX does _not_ support the following features:

- transactions
- pessimistic locking via `LockMode`
- `@ElementCollection` and `@ManyToMany`
- `@OneToMany` without `mappedBy` 
- `GenerationType.IDENTITY` (autoincrement columns), and custom id 
  generation strategies
- `@NamedEntityGraph`
- transparent lazy loading
- eager select fetching, for example `@ManyToOne(fetch=EAGER) @Fetch(SELECT)`
- criteria queries

Instead, use the following supported features:

- optimistic locking with `@Version`
- `@OneToMany(mappedBy=...)` together with `@ManyToOne`
- `SEQUENCE` or `TABLE` id generation
- `@FetchProfile`
- explicit lazy loading via `RxSession.fetch(entity.association)`, which 
  returns a `CompletionStage`

Note that you should not use Hibernate RX with a second-level cache 
implementation which performs blocking IO, for example passivation to the
filesystem or distributed replication.

Currently only PostgreSQL is supported. Support for MySQL is coming soon!
