package davidantass.vitta.infrastructure.exception;

import davidantass.vitta.domain.BusinessRuleException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.NoSuchElementException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({NoSuchElementException.class, EntityNotFoundException.class, NoResourceFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound() {
        return "error/404";
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class, BindException.class})
    public ModelAndView handleInvalidRequest() {
        return errorPage(HttpStatus.BAD_REQUEST, "Check the required fields and the format of the supplied values.");
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ModelAndView handleBusinessRule(BusinessRuleException exception) {
        return errorPage(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ModelAndView handleDataConflict() {
        return errorPage(HttpStatus.CONFLICT, "The operation conflicts with existing records. Check for duplicate data or linked appointments.");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ModelAndView handleAccessDenied() {
        return errorPage(HttpStatus.FORBIDDEN, "You do not have permission to perform this operation.");
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleUnexpectedError(Exception exception, HttpServletRequest request, HttpServletResponse response) {
        // Preserve Spring MVC status codes and headers, such as 405 and Allow.
        if (exception instanceof ErrorResponse errorResponse) {
            errorResponse.getHeaders().forEach((name, values) -> values.forEach(value -> response.addHeader(name, value)));
            return errorPage(errorResponse.getStatusCode(), "The request could not be processed.");
        }

        log.error("Unexpected error while processing {} {}", request.getMethod(), request.getRequestURI(), exception);
        var view = new ModelAndView("error/500");
        view.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        return view;
    }

    private ModelAndView errorPage(HttpStatusCode status, String message) {
        var view = new ModelAndView("error/request");
        view.setStatus(status);
        view.addObject("statusCode", status.value());
        view.addObject("message", message);
        return view;
    }

}
