package cleb.uploading.validating;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import java.io.File;
import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This servlet validates new fb2 books.
 */
public class FB2Validator extends HttpServlet implements IValidator {

    private static final long serialVersionUID = 1L;

    // Logger for this class
    private static final Logger logger = LogManager
        .getLogger(FB2Validator.class.getName());

    private String tempFolderPath;

    private Document book;

    /**
     * Initializes temporary directory.
     */
    @Override
    public void init() throws ServletException {
        // Directory for temporary storing uploaded books - till it's checked by
        // this servlet
        tempFolderPath = getServletContext()
            .getInitParameter("file-temp-upload");

        logger.info("FB2Validator initialized");
    }

    @Override
    protected void doPost(HttpServletRequest request,
        HttpServletResponse response) throws ServletException, IOException {
        String tempBookPath = tempFolderPath
            + (String) request.getAttribute("file");
        File tempBookFile = new File(tempBookPath);

        if (validateBook(tempBookFile)) {
            request.setAttribute("book", book);
            RequestDispatcher dispatcher = request
                .getRequestDispatcher("/FB2Saver");
            dispatcher.forward(request, response);
        } else {
            // TODO inform user about malformed book
        }
    }

    /**
     * This method validates given fb2 book.
     *
     * @param file
     *        fb2 book to validate
     * @return true, if given file is valid fb2 book and false - otherwise
     */
    // Currently this method simply builds DOM tree from given file (fb2
    // internally uses XML) and catches exceptions, if no exception is thrown
    // during building - file should be valid
    // TODO add validation against actual xsd schema
    // For now, not all books pass validation against xsd, maybe it is caused by
    // using different schemas while creating those books
    @Override
    public boolean validateBook(File file) {
        boolean validated = false;

        SAXBuilder builder = new SAXBuilder();
        try {
            book = builder.build(file);

            logger.info("File \"{}\" successfully validated", file.getName());

            validated = true;
        } catch (JDOMException e) {
            logger.error("File \"{}\" is not a valid fb2 book", file.getName(),
                e);

            validated = false;
        } catch (IOException e) {
            logger.error("Can not validate book \"{}\"", file.getName(), e);

            validated = false;
        }

        return validated;
    }

}
