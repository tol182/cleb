package cleb.uploading.saving;

import static cleb.uploading.util.JDBCPoolUtil.getConnection;

import org.apache.commons.io.FileUtils;
import org.hibernate.Session;
import org.hibernate.SessionBuilder;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;

import java.io.File;
import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import cleb.book.Book;

/**
 * This interface contains necessary method to get information about uploaded
 * book. Any concrete saver class should implement this interface and override
 * its getBasicInfo method based on book type.
 *
 * This interface also contains default methods for storing book in database and
 * in storing directory.
 */
// TODO remove e.printstacktraces
public interface ISaver {

    /**
     * Implementation of this method should retrieve all information about
     * uploaded book according to its type. book parameter should be cast to its
     * actual type depending on book type and exctraction logic.
     *
     * @param request
     *        HttpServletRequest from doPost method to retrieve information
     *        about file
     * @param book
     *        Object representing actual book
     *
     * @return value returned by storeInDB method
     */
    public boolean getBasicInfo(HttpServletRequest request, Object book);

    /**
     * Stores information about uploaded book in a database.
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
     */
    @SuppressWarnings("rawtypes")
    public default boolean storeInDB(String fileName, String md5, Long fileSize,
        String fileType, String genre, String authorFirstName,
        String authorLastName, String title, String seqName, String seqNumber,
        String published, String uploadedBy) {

        // Create new Book object
        Book book = new Book(fileName, md5, fileSize, fileType, genre,
            authorFirstName, authorLastName, title, seqName, seqNumber,
            published, uploadedBy);

        // Create session and transaction
        SessionFactory factory = new Configuration().configure()
            .buildSessionFactory();

        SessionBuilder builder = factory.withOptions();
        // Supply connection from connection pool
        builder.connection(getConnection());

        Transaction transaction = null;

        try (Session session = builder.openSession()) {
            transaction = session.beginTransaction();
            session.save(book);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }

            e.printStackTrace();

            return false;
        }

        return true;
    }

    /**
     * Moves book from temporary to storing directory.
     *
     * @param srcFile
     *        Path to file in temporary directory
     * @param destFile
     *        Path to file in storing directory
     */
    public default void storeInDir(String srcFile, String destFile) {
        try {
            FileUtils.moveFile(new File(srcFile), new File(destFile));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Implementation of this method should get cover from uploaded book and
     * store it in special directory for covers. Cover should be saved under
     * specific name - it constists of the book file name and image extension
     * (jpeg/png).
     *
     * @param book
     *        Object representing actual book
     * @param name
     *        Book file name
     */
    public void saveCover(Object book, String name);

}
