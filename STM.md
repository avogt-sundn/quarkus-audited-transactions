
Both @Transactional annotations work

    import javax.transaction.Transactional;
    import org.jboss.stm.annotations.Transactional;

So you can mix JPA transactions and STM transactions. 
Actually the transaction manager - [Narayana](https://narayana.io/architecture/index.html) (formerly/also known as Arjuna) - is the same.

