package com.project.springbootlibrary.dao;

import com.project.springbootlibrary.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.web.bind.annotation.RequestParam;

public interface MessageRepository extends JpaRepository<Message,Long> {
    // the user can get all the messages that he asked and the answers
    Page<Message> findByuserEmail(@RequestParam("userEmail") String userEmail, Pageable pageable);

    // the admin can find the questions which have no answer for all users
    Page<Message> findByClosed(@RequestParam("closed") boolean closed , Pageable pageable);
}
