package com.project.springbootlibrary.service;

import com.project.springbootlibrary.dao.BookRepository;
import com.project.springbootlibrary.dao.CheckoutRepository;
import com.project.springbootlibrary.dao.HistoryRepository;
import com.project.springbootlibrary.dao.PaymentRepository;
import com.project.springbootlibrary.entity.Book;
import com.project.springbootlibrary.entity.Checkout;
import com.project.springbootlibrary.entity.History;
import com.project.springbootlibrary.entity.Payment;
import com.project.springbootlibrary.responsemodels.ShelfCurrentLoansResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;


@Service
@Transactional
public class BookService {

    private BookRepository bookRepository;

    private CheckoutRepository checkoutRepository;
    private HistoryRepository historyRepository;
    private PaymentRepository paymentRepository;


    public BookService(HistoryRepository historyRepository, BookRepository bookRepository,
                       CheckoutRepository checkoutRepository, PaymentRepository paymentRepository) {
        this.bookRepository = bookRepository;
        this.checkoutRepository = checkoutRepository;
        this.historyRepository = historyRepository;
        this.paymentRepository = paymentRepository;
    }

    public Book checkoutBook(String userEmail, Long bookId) throws Exception {

        Optional<Book> book = bookRepository.findById(bookId);

        Checkout validateCheckout = checkoutRepository.findByUserEmailAndBookId(userEmail, bookId);

        if (!book.isPresent() || validateCheckout != null || book.get().getCopiesAvailable() <= 0) {
            throw new Exception("Book doesn't exist or already checked out by user");
        }

        List<Checkout> currentBookCheckout = checkoutRepository.findBooksByUserEmail(userEmail);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        boolean bookNeedsReturned = false;

        for (Checkout checkout : currentBookCheckout) {
            Date d1 = sdf.parse(checkout.getReturnDate());
            Date d2 = sdf.parse((LocalDate.now()).toString());
            TimeUnit time = TimeUnit.DAYS;

            double differenceInTime = time.convert(d1.getTime() - d2.getTime(), TimeUnit.MILLISECONDS);
            if (differenceInTime < 0) {
                bookNeedsReturned = true;
                break;
            }
        }
        Payment userPayment = paymentRepository.findByuserEmail(userEmail);
        if (userPayment != null && userPayment.getAmount() > 0 || (userPayment != null && bookNeedsReturned)) {
            throw new Exception("Outstanding fees");
        }
        if (userPayment == null) {
            Payment payment = new Payment();
            payment.setAmount(00.00);
            payment.setUserEmail(userEmail);
            paymentRepository.save(payment);
        }

        book.get().setCopiesAvailable(book.get().getCopiesAvailable() - 1);
        bookRepository.save(book.get());

        Checkout checkout = new Checkout(
                userEmail,
                LocalDate.now().toString(),
                LocalDate.now().plusDays(7).toString(),
                book.get().getId()
        );

        checkoutRepository.save(checkout);

        return book.get();
    }

    public int currentLoansCount(String userEmail) {
        return checkoutRepository.findBooksByUserEmail(userEmail).size();
    }

    public Boolean checkoutBookByUser(String userEmail, Long bookId) {
        Checkout validateCheckout = checkoutRepository.findByUserEmailAndBookId(userEmail, bookId);
        if (validateCheckout != null) {
            return true;
        } else {
            return false;
        }
    }

    public List<ShelfCurrentLoansResponse> currentLoans(String userEmail) throws Exception {

        List<ShelfCurrentLoansResponse> shelfCurrentLoansResponses = new ArrayList<>();


        List<Checkout> checkoutList = checkoutRepository.findBooksByUserEmail(userEmail);
        List<Long> bookIdList = new ArrayList<>();

        for (Checkout i : checkoutList) {
            bookIdList.add(i.getBookId());
        }

        List<Book> books = bookRepository.findBooksByBookIds(bookIdList);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");


        for (Book book : books) {
            Optional<Checkout> checkout = checkoutList.stream()
                    .filter(x ->
                            x.getBookId() == book.getId()).findFirst();
            if (checkout.isPresent()) {
                Date d1 = sdf.parse(checkout.get().getReturnDate());
                Date d2 = sdf.parse(LocalDate.now().toString());

                TimeUnit time = TimeUnit.DAYS;
                long difference_Tn_Time = time.convert(d1.getTime() - d2.getTime(), TimeUnit.MILLISECONDS);

                shelfCurrentLoansResponses.add(new ShelfCurrentLoansResponse(book, (int) difference_Tn_Time));

            }
        }
        return shelfCurrentLoansResponses;
    }

    public void returnBook(String userEmail, Long bookId) throws Exception {
        Optional<Book> book = bookRepository.findById(bookId);
        Checkout validateCheckout = checkoutRepository.findByUserEmailAndBookId(userEmail, bookId);

        if (!book.isPresent() || validateCheckout == null) {
            throw new Exception("Book doesn't exist or not checkout out by user");
        }
        book.get().setCopiesAvailable(book.get().getCopiesAvailable() + 1);
        bookRepository.save(book.get());

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date d1 = sdf.parse(validateCheckout.getReturnDate());
        Date d2 = sdf.parse(LocalDate.now().toString());
        TimeUnit time = TimeUnit.DAYS;
        double differenceInTime = time.convert(d1.getTime()- d2.getTime(),TimeUnit.MILLISECONDS);
        if (differenceInTime < 0) {
            Payment payment = paymentRepository.findByuserEmail(userEmail);
            payment.setAmount(payment.getAmount() + (differenceInTime * -1));
            paymentRepository.save(payment);
        }
        checkoutRepository.deleteById(validateCheckout.getId());
        History history = new History(
                userEmail
                , validateCheckout.getCheckoutDate()
                , LocalDate.now().toString()
                , book.get().getTitle()
                , book.get().getAuthor()
                , book.get().getDescription()
                , book.get().getImg());
        historyRepository.save(history);
    }

    public void renewLoan(String userName, Long bookId) throws Exception {
        Checkout validateCheckout = checkoutRepository.findByUserEmailAndBookId(userName, bookId);
        if (validateCheckout == null) {
            throw new Exception("Book does not exist or not checked out by this user ");
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        Date d1 = sdf.parse(validateCheckout.getReturnDate());
        Date d2 = sdf.parse(LocalDate.now().toString());

        if (d1.compareTo(d2) > 0 || d1.compareTo(d2) == 0) {
            validateCheckout.setReturnDate(LocalDate.now().plusDays(7).toString());
            checkoutRepository.save(validateCheckout);
        }
    }
}