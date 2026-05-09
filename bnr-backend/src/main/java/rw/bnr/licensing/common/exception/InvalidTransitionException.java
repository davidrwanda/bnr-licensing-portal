package rw.bnr.licensing.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author David NTAMAKEMWA
 */
@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class InvalidTransitionException extends RuntimeException {

    public InvalidTransitionException(String message) {
        super(message);
    }
}
