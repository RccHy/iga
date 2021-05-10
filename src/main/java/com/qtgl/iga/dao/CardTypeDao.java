package com.qtgl.iga.dao;

import com.qtgl.iga.bo.CardType;

import java.util.List;

public interface CardTypeDao {
    List<CardType> findAll(String domain);
}
