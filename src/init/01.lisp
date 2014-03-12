;; General-purpose functions contributed by Jean-Pierre Gaillardin
;; May 2005.
(require '(unsorted strings))
(setq *package* (find-package "SYSTEM"))

;; Accessors 
(defun caar (l) (car (car l)))
(defun cadr (l) (car (cdr l)))
(defun cdar (l) (cdr (car l)))
(defun cddr (l) (cdr (cdr l)))
(defun caaar (l) (car (car (car l))))
(defun caadr (l) (car (car (cdr l))))
(defun cadar (l) (car (cdr (car l))))
(defun caddr (l) (car (cdr (cdr l))))
(defun cdaar (l) (cdr (car (car l))))
(defun cdadr (l) (cdr (car (cdr l))))
(defun cddar (l) (cdr (cdr (car l))))
(defun cdddr (l) (cdr (cdr (cdr l))))
(defun caaaar (l) (car (car (car (car l)))))
(defun caaadr (l) (car (car (car (cdr l)))))
(defun caadar (l) (car (car (cdr (car l)))))
(defun caaddr (l) (car (car (cdr (cdr l)))))
(defun cadaar (l) (car (cdr (car (car l)))))
(defun cadadr (l) (car (cdr (car (cdr l)))))
(defun caddar (l) (car (cdr (cdr (car l)))))
(defun cadddr (l) (car (cdr (cdr (cdr l)))))
(defun cdaaar (l) (cdr (car (car (car l)))))
(defun cdaadr (l) (cdr (car (car (cdr l)))))
(defun cdadar (l) (cdr (car (cdr (car l)))))
(defun cdaddr (l) (cdr (car (cdr (cdr l)))))
(defun cddaar (l) (cdr (cdr (car (car l)))))
(defun cddadr (l) (cdr (cdr (car (cdr l)))))
(defun cdddar (l) (cdr (cdr (cdr (car l)))))
(defun cddddr (l) (cdr (cdr (cdr (cdr l)))))

(export '(caar cadr cdar cddr caaar caadr cadar caddr cdaar cdadr cddar cdddr caaaar caaadr caadar caaddr cadaar cadadr caddar cadddr cdaaar cdaadr cdadar cdaddr cddaar cddadr cdddar cddddr))

; http://www.lispworks.com/documentation/lw51/CLHS/Body/f_firstc.htm
(defun first  (l) (car l))
(defun second (l) (car (cdr l)))
(defun third  (l) (car (cddr l)))
(defun fourth (l) (car (cdddr l)))
(defun fifth  (l) (car (cddddr l)))
(defun sixth  (l) (car (cdr (cddddr l))))
(defun seventh(l) (car (cddr (cddddr l))))
(defun eighth (l) (car (cdddr (cddddr l))))
(defun ninth  (l) (car (cddddr (cddddr l))))
(defun tenth  (l) (car (cdr (cddddr (cddddr l)))))

(defun nth (n list) (elt list (- n 1)))

(export '(first second third fourth fifth sixth seventh eighth ninth tenth nth))

; http://clhs.lisp.se/Body/f_zerop.htm
(defun zerop (x) (= x 0))
(export '(zerop))

;; ------------------------------------------------
;; GENSYM
;; ------------------------------------------------
(defparameter *gensym-counter* 1800)

(defun gensym nil
 (defparameter *gensym-counter* (+ 1 *gensym-counter*))
 (intern (concatenate 'STRING "#:G" *gensym-counter*)))

;; ------------------------------------------------
;; ENDP     (simple version)
;; ------------------------------------------------
(defun endp (l)
  (null l)
  )

;; ------------------------------------------------
;; ERROR    (very simple version)
;; ------------------------------------------------
; should be a primitive and throw a Java Exception

(defun error (&rest l)
  (print "**** ERROR ****")
  (print l)
  )
  
(export '(error gensym endp))
;; todo: These should all eventually move into the main code.
