;; Simple general functions from CLTL2
;; Contributed by Ola Bini
;; May 2005

(setq *package* (find-package "SYSTEM"))

;; This is a dummy implementation, since we don't need the real thing right now. It just expands the body into an progn right now.
(defmacro eval-when (situation &rest body)
  `(progn ,@body))

(defmacro in-package (name)
  `(eval-when (:compile-toplevel :load-toplevel :execute) (setq *package* (find-package ,name))))

(in-package "SYSTEM")

(defmacro unless (test &rest body)
  `(and (not ,test) (progn ,@body)))

(defmacro when (test &rest body)
  `(and ,test (progn ,@body)))

(defmacro let* (vars &rest body)
  (if (endp vars)
      `(progn ,@body)
      `(let (,(car vars)) (let* ,(cdr vars) ,@body))))

(defmacro return (value)
  `(return-from nil ,value))

(defmacro loop (&rest body)
  (let ((tagSym (gensym)))
    `(block nil
      (tagbody
         ,tagSym
         ,@body
         (go ,tagSym)))))

(defmacro dotimes (init &rest body)
  (let ((var (first init))
        (uptoForm (second init))
        (resultForm (third init))
        (uptoSym (gensym)))
    `(let ((,uptoSym ,uptoForm)
           (,var 0))
      (loop
       (when (not (< ,var ,uptoSym))
         (return ,resultForm))
       ,@body
       (setq ,var (1+ ,var))))))

(defmacro dolist (init &rest body)
  (let ((var (first init))
        (listForm (second init))
        (resultForm (third init))
        (listSym (gensym)))
    `(let* ((,listSym ,listForm)
            (,var (first ,listSym)))
      (loop
       (when (endp ,var)
         (return ,resultForm))
       ,@body
       (setq ,listSym (rest ,listSym))
       (setq ,var (first ,listSym))))))       

(defun terpri ()
  (princ #\Newline))

(export '(unless when let* in-package eval-when return loop dotimes dolist terpri))