package authorization.errors

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class ExceptionHandler {
    @ExceptionHandler(UserNotFoundException::class)
    fun handle(ex: UserNotFoundException): ResponseEntity<Any> {
        return ResponseEntity.badRequest().body(ex.message)
    }
}
