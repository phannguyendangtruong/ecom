package org.ecom.service;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Transactional
public abstract class BaseServiceImpl<T, ID> implements BaseService<T, ID>{
    protected final JpaRepository<T, ID> repository;

    protected BaseServiceImpl(JpaRepository<T, ID> repository){
        this.repository = repository;
    }

    @Override
    public T save(T entity){
        return repository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<T> findById(ID id){
        return repository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<T> findAll(){
        return repository.findAll();
    }

    @Override
    public void deleteById(ID id){
        repository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existById(ID id){
        return repository.existsById(id);
    }
}
