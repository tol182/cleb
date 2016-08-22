package cleb.uploading.validating;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;

/**
 * This servlet validates new epub books.
 */
public class EPUBValidator extends HttpServlet implements IValidator {

    private static final long serialVersionUID = 1L;

    // Logger for this class
    private static final Logger logger = LogManager
        .getLogger(EPUBValidator.class.getName());

    private String tempFolderPath;

    private String fileName;

    private ZipFile book;

    @Override
    public void init() throws ServletException {
        // Directory for temporary storing uploaded books - till it's checked by
        // this servlet
        tempFolderPath = getServletContext()
            .getInitParameter("file-temp-upload");

        logger.info("EPUBValidator initialized");
    }

    @Override
    protected void doPost(HttpServletRequest request,
        HttpServletResponse response) throws ServletException, IOException {

        fileName = (String) request.getAttribute("file");

        String tempBookPath = tempFolderPath + fileName;
        File tempBookFile = new File(tempBookPath);

        if (validateBook(tempBookFile)) {
            request.setAttribute("book", book);
            RequestDispatcher dispatcher = request
                .getRequestDispatcher("/EPUBSaver");
            dispatcher.forward(request, response);
        } else {
            // TODO inform user about malformed book
        }
    }

    @Override
    public boolean validateBook(File file) {
        boolean validated = false;

        // Valid epub book should be valid zip archive with certain directory
        // structure inside
        try {
            book = new ZipFile(file);

            if (book.isValidZipFile()) {
                try {
                    // Any valid epub book should contain these two files
                    book.getFileHeader("META-INF/container.xml").getFileName();
                    book.getFileHeader("mimetype").getFileName();

                    validated = true;

                    logger.info("Book \"{}\" successfully validated", fileName);
                } catch (NullPointerException e) {
                    validated = false;

                    logger.error("Book \"{}\" is not a valid epub book",
                        fileName, e);
                }
            } else {
                validated = false;

                logger.warn("Book \"{}\" is not a valid epub book", fileName);
            }
        } catch (ZipException e) {
            logger.error("Can not validate book \"{}\"", fileName, e);
        }

        return validated;
    }

}
