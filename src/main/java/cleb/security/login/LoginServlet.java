package cleb.security.login;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.LockedAccountException;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templateresolver.ServletContextTemplateResolver;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This servlet class manages users log-in ang log-out to/from site.
 */
public class LoginServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    // Logger for this class
    private static final Logger logger = LogManager
        .getLogger(LoginServlet.class.getName());

    private String errorDesc;

    private ServletContext servletContext;
    private ServletContextTemplateResolver templateResolver;
    private TemplateEngine templateEngine;

    @Override
    public void init() throws ServletException {
        // Load properties
        Properties properties = new Properties();

        try (InputStream propIn = getServletContext()
            .getResourceAsStream("/WEB-INF/classes/props.properties")) {
            properties.load(propIn);
        } catch (IOException e) {
            logger.error("Can not load properties", e);
        }

        // Error description when user can not be logged-in
        errorDesc = properties.getProperty("login-servlet-error");

        // Initialize Thymeleaf for this servlet
        servletContext = getServletContext();
        templateResolver = new ServletContextTemplateResolver(servletContext);
        templateResolver.setTemplateMode("HTML5");
        // Prefix and suffix for template
        templateResolver.setPrefix("/WEB-INF/templates/");
        templateResolver.setSuffix(".html");
        templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);
    }

    @Override
    protected void doGet(HttpServletRequest request,
        HttpServletResponse response) throws ServletException, IOException {

        response.setContentType("text/html");

        // If called with parameter 'logout' - logs out current user
        if (request.getParameter("logout") != null) {
            // Log out current user
            Subject currentUser = SecurityUtils.getSubject();
            String userName = currentUser.getPrincipal().toString();
            currentUser.logout();

            logger.info("User \"{}\" logged out", userName);
        }

        WebContext webContext = new WebContext(request, response,
            servletContext, request.getLocale());

        // Get current user
        Subject currentUser = SecurityUtils.getSubject();

        if (currentUser.isAuthenticated()) {
            // No need to login again
            // Show index.html page
            response.sendRedirect("index");
        } else {
            // Show login.html page
            templateEngine.process("login", webContext, response.getWriter());
        }
    }

    @Override
    protected void doPost(HttpServletRequest request,
        HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html");

        // Get parameters
        String email = request.getParameter("email");
        String password = request.getParameter("password");
        Boolean rememberMe = Boolean
            .valueOf(request.getParameter("rememberMe"));

        // Check for both email and password being not empty and not null
        // This is almost reduntant with using "required" attribute on each
        // input tag
        if (email.length() == 0 || password.length() == 0 || email == null
            || password == null) {

            logger.warn("Empty user email and/or password");
        } else {
            // Try to login
            boolean loggedIn = tryLogin(email, password, rememberMe);

            if (loggedIn) {
                // Redirect to index page
                response.sendRedirect("index");
            } else {
                // Inform user about error
                request.setAttribute("errordesc", errorDesc);
                request.setAttribute("previouspage", "/login");

                RequestDispatcher dispatcher = getServletContext()
                    .getRequestDispatcher("/error");

                dispatcher.forward(request, response);
            }
        }
    }

    /**
     * Tries to login user with provided email and password.
     *
     * @param email {@code String} with user entered email.
     * @param password {@code String} with user entered password.
     * @param rememberMe {@code Boolean} with true if remember me was checked
     *        and false otherwise.
     *
     * @return {@code true}, if user was successfully logged-in, otherwise -
     *         {@code false}.
     */
    private boolean tryLogin(String email, String password,
        Boolean rememberMe) {

        boolean loggedIn = false;
        // Get current user
        Subject currentUser = SecurityUtils.getSubject();

        if (!currentUser.isAuthenticated()) {
            // Collect user principals and credentials
            UsernamePasswordToken token = new UsernamePasswordToken(email,
                password);
            // Set remember me status
            token.setRememberMe(rememberMe);

            try {
                // Try to login
                currentUser.login(token);

                // Save current username in the session
                currentUser.getSession().setAttribute("username", email);

                logger.info("User \"{}\" successfully logged-in", email);

                loggedIn = true;
            } catch (UnknownAccountException e) {
                loggedIn = false;

                logger.error("User \"{}\" not found", email, e);
            } catch (IncorrectCredentialsException e) {
                loggedIn = false;

                logger.error("Incorrect password for user \"{}\"", email, e);
            } catch (LockedAccountException e) {
                loggedIn = false;

                logger.error("User \"{}\" account is locked", email, e);
            }
        } else {
            // Alredy logged in
            loggedIn = false;

            logger.warn("User \"{}\" already logged-in", email);
        }

        return loggedIn;
    }

}
