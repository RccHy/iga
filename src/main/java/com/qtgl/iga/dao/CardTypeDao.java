package com.qtgl.iga.dao;

import com.qtgl.iga.bo.CardType;

import java.util.List;

public interface CardTypeDao {
    List<CardType> findAllUser(String domain);
    List<CardType> findAllFromIdentity(String domain);
    void  initialization(String domain);
}
