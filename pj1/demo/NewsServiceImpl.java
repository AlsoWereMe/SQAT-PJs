package com.demo.service.impl;

import com.demo.dao.NewsDao;
import com.demo.entity.News;
import com.demo.service.NewsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Pageable;

@Service
public class NewsServiceImpl implements NewsService {
    @Autowired
    private NewsDao newsDao;

    @Override
    public Page<News> findAll(Pageable pageable) {
        if (pageable == null) {
            throw new IllegalArgumentException("Pageable cannot be null");
        }
        return newsDao.findAll(pageable);
    }

    @Override
    public News findById(int newsID) {
        if (newsID <= 0) {
            throw new IllegalArgumentException("Invalid news ID");
        }
        return newsDao.getOne(newsID);
    }


    @Override
    public int create(News news) {
        if (news == null) {
            throw new IllegalArgumentException("News cannot be null");
        }
        return newsDao.save(news).getNewsID();
    }

    @Override
    public void delById(int newsID) {
        if (newsID <= 0) {
            throw new IllegalArgumentException("Invalid news ID");
        }
        newsDao.deleteById(newsID);
    }

    @Override
    public void update(News news) {
        if (news == null) {
            throw new IllegalArgumentException("News cannot be null");
        }
        newsDao.save(news);
    }
}
