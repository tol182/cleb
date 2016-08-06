package cleb.uploading;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This servlet checks if newly uploading book is not already in library.
 */
// TODO remove e.printStackTrace's
public class DuplicateChecker extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private String tempFolderPath;
    private String folderPath;

    private String dbURL;
    private String dbuser;
    private String dbpass;

    // Prepared query (statement)
    //@formatter:off
    private final String query = "SELECT COUNT(*) "
	    + "FROM books "
	    + "WHERE md5sum = ? "
	    + "AND file_size = ?";
    //@formatter:on

    /**
     * Initializes uploading parameters - temporary folder and storing folder,
     * jdbc driver and db credentials.
     */
    @Override
    public void init() {
	// Directory for temporary storing uploaded books - till it's checked by
	// this servlet
	tempFolderPath = getServletContext()
		.getInitParameter("file-temp-upload");
	// Directory to store uploaded books
	folderPath = getServletContext().getInitParameter("file-store");

	// Initialize PostgreSQl driver
	try {
	    Class.forName("org.postgresql.Driver");
	} catch (ClassNotFoundException e) {
	    e.printStackTrace();
	}

	// Initialize database URL for driver
	dbURL = getServletContext().getInitParameter("database");

	// Initialize database user
	dbuser = getServletContext().getInitParameter("dbuser");

	// Initialize database user password
	dbpass = getServletContext().getInitParameter("dbpass");
    }

    @Override
    protected void doGet(HttpServletRequest request,
	    HttpServletResponse response) throws ServletException, IOException {
    }

    @Override
    protected void doPost(HttpServletRequest request,
	    HttpServletResponse response) throws ServletException, IOException {

	String tempBookPath = tempFolderPath + request.getParameter("book");
	File tempBookFile = new File(tempBookPath);

	String md5sum = getMd5sum(tempBookFile);

	if (md5sum != null) {
	    long fileSize = tempBookFile.length();

	    if (checkBookPresence(md5sum, fileSize)) {
		// There is already this book in library, inform user
		// TODO add forwarding to page and inform user
	    } else {
		// This book is new, proceed with XML validation
		RequestDispatcher dispatcher = request
			.getRequestDispatcher("/Validator");
		dispatcher.forward(request, response);
	    }
	} else {
	    // TODO add forwarding to page with error
	}
    }

    /**
     * This method calculates temporarily uploaded book MD5 sum.
     *
     * @param file
     *        Previously uploaded book in temp folder
     * @return String representing this book MD5 sum value
     */
    private String getMd5sum(File file) {
	String md5sum = null;

	try (FileInputStream fileIn = new FileInputStream(file);
	     BufferedInputStream bufferIn = new BufferedInputStream(fileIn);) {

	    md5sum = DigestUtils.md5Hex(IOUtils.toByteArray(bufferIn));
	} catch (IOException e) {
	    e.printStackTrace();
	}

	return md5sum;
    }

    /**
     * This method connects to database and checks if there is already a book
     * with given md5 sum and file size.
     *
     * @param md5sum
     *        String representing new book md5 sum to check among already
     *        uploaded books
     * @param fileSize
     *        long representing new book file size to check among already
     *        uploaded books
     * @return true - if database already contains book with given md5 sum and
     *         file size, otherwise - false
     */
    private boolean checkBookPresence(String md5sum, long fileSize) {
	boolean present = false;

	try {
	    Connection connection = DriverManager.getConnection(dbURL, dbuser,
		    dbpass);

	    PreparedStatement pstatement = connection.prepareStatement(query);
	    pstatement.setString(1, md5sum);
	    pstatement.setLong(2, fileSize);

	    ResultSet results = pstatement.executeQuery();

	    while (results.next()) {
		if (results.getInt(1) > 0) {
		    present = true;
		} else {
		    present = false;
		}
	    }
	} catch (SQLException e) {
	    e.printStackTrace();
	}

	return present;
    }

}