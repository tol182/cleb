package cleb.book.dao;

import static cleb.uploading.util.JDBCPoolUtil.closeConnection;
import static cleb.uploading.util.JDBCPoolUtil.getConnection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionBuilder;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;

import java.sql.Connection;
import java.util.List;

import cleb.book.Book;

/**
 * This utility class provides methods for working with books in database.
 */
public class BookDAO {

    // Logger for this class
    private static final Logger logger = LogManager
        .getLogger(BookDAO.class.getName());

    /**
     * Saves information about uploaded book in a database.
     *
     * @param fileName
     * @param md5
     * @param fileSize
     * @param fileType
     * @param genre
     * @param authorFirstName
     * @param authorLastName
     * @param title
     * @param seqName
     * @param seqNumber
     * @param published
     * @param uploadedBy
     *
     * @return true if transaction was successful and false - otherwise.
     *
     * @see cleb.uploading.util.JDBCPoolUtil#closeConnection(Connection)
     * @see cleb.uploading.util.JDBCPoolUtil#getConnection()
     */
    @SuppressWarnings("rawtypes")
    public static boolean storeInDB(String fileName, String md5, Long fileSize,
        String fileType, String genre, String authorFirstName,
        String authorLastName, String title, String seqName, String seqNumber,
        String published, String uploadedBy) {

        boolean stored = false;

        // Create new Book object
        // Paths for book and cover will be added later
        Book book = new Book(fileName, md5, fileSize, fileType, genre,
            authorFirstName, authorLastName, title, seqName, seqNumber,
            published, uploadedBy, "", "");

        // Create session and transaction
        SessionFactory factory = new Configuration().configure()
            .buildSessionFactory();

        SessionBuilder builder = factory.withOptions();
        // Supply connection from connection pool
        Connection connection = getConnection();
        builder.connection(connection);

        Transaction transaction = null;

        try (Session session = builder.openSession()) {
            transaction = session.beginTransaction();
            session.save(book);
            transaction.commit();

            stored = true;
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }

            stored = false;

            logger.error(
                "Can not store information about book \"{}\" in database",
                fileName, e);
        } finally {
            closeConnection(connection);
        }

        return stored;
    }

    // TODO add javadoc
    @SuppressWarnings({ "deprecation", "rawtypes" })
    public static void addPaths(String fileName, String filePath,
        String coverPath) {

        // Create session and transaction
        SessionFactory factory = new Configuration().configure()
            .buildSessionFactory();

        SessionBuilder builder = factory.withOptions();
        // Supply connection from connection pool
        Connection connection = getConnection();
        builder.connection(connection);

        Transaction transaction = null;

        try (Session session = builder.openSession()) {
            transaction = session.beginTransaction();
            Book book = (Book) session.createQuery("from Book where fileName=?")
                .setParameter(0, fileName).uniqueResult();
            book.setFilePath(filePath);
            book.setCoverPath(coverPath);
            session.update(book);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }

            logger.error("Can not add paths for book \"{}\" in database",
                fileName, e);
        } finally {
            closeConnection(connection);
        }
    }

    /**
     * Returns {@code List} with all {@code Book} objects stored in database.
     *
     * @return {@code List} with books.
     */
    @SuppressWarnings({ "deprecation", "rawtypes", "unchecked" })
    public static List<Book> getAllBooks() {
        List books = null;

        // Create session and transaction
        SessionFactory factory = new Configuration().configure()
            .buildSessionFactory();

        SessionBuilder builder = factory.withOptions();
        // Supply connection from connection pool
        Connection connection = getConnection();
        builder.connection(connection);

        Transaction transaction = null;

        try (Session session = builder.openSession()) {
            transaction = session.beginTransaction();
            books = session.createQuery("from Book").list();
            transaction.commit();
        } catch (Exception e) {
            logger.error(
                "Can not retrieve information about all books from database",
                e);
        } finally {
            closeConnection(connection);
        }

        return books;
    }

}