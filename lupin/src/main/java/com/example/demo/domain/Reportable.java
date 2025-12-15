package com.example.demo.domain;

import com.example.demo.domain.entity.User;

public interface Reportable {
    Long getId();
    User getWriter();
}
