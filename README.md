# Revisions and Transactions in Quarkus 

Use case: 
- you need all changes on your entities to be logged into an extra audit log.

Run the [AuditResourceTest.java](src/test/java/org/acme/hibernate/orm/envers/AuditResourceTest.java) and look at the code. The AuditResource gives you a history of the changes done during test like this:

    mvn docker:start && sleep 5 && mvn compile test

This is the logged history:

    {
        "fetchDate": "2020-04-25T17:57:50",
        "history": [
            {
                "info": {
                    "id": 1,
                    "revisionDate": "2020-04-25T15:57:49",
                    "timestamp": 1587830269258,
                    "username": "your-name-Sat Apr 25 17:57:49 CEST 2020"
                },
                "ref": {
                    "name": "one",
                    "uuid": "048a636e-a729-4242-81cc-4d6e26081827",
                    "version": 0
                },
                "revision": 1
            },
            {
                "info": {
                    "id": 2,
                    "revisionDate": "2020-04-25T15:57:50",
                    "timestamp": 1587830270299,
                    "username": "your-name-Sat Apr 25 17:57:50 CEST 2020"
                },
                "ref": {
                    "name": "two",
                    "uuid": "048a636e-a729-4242-81cc-4d6e26081827",
                    "version": 0
                },
                "revision": 2
            },
            {
                "info": {
                    "id": 3,
                    "revisionDate": "2020-04-25T15:57:50",
                    "timestamp": 1587830270395,
                    "username": "your-name-Sat Apr 25 17:57:50 CEST 2020"
                },
                "ref": {
                    "name": "three",
                    "uuid": "048a636e-a729-4242-81cc-4d6e26081827",
                    "version": 0
                },
                "revision": 3
            }
        ]
    }


We are using:
* Quarkus 1.3.2.Final
* Hibernate ORM
* Hibernate Envers
* Panache Entity
* Narayana Transactions (Arjuna)

Everything is done in Unit tests (@QuarkusTest tests).

## Hibernate Envers

- see [ENVERS.md](ENVERS.md)

## Software transactional memory STM

- see [STM.md](STM.md)

## On testing with Quarkus

- see [TESTING.md](TESTING.md)

## Best practices for Rest implementation

- see [REST.md](REST.md)


## Known effects
#### @Version value is lost in the revisions

When a new revision is being created, the revision table will receive a new row with a copy of all values present in the current entity.
The @Version version field will always stay at 0.


---
Base material taken from:

* Quarkus guide: https://quarkus.io/guides/software-transactional-memory
