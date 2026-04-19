package com.xushu.webtable.utils;

public class CurrentHolder {
    private static ThreadLocal<Integer> holder=new ThreadLocal<>();
    public static void set(Integer id){
        holder.set(id);
    }
    public static Integer get(){
        return holder.get();
    }
    public static void remove(){
         holder.remove();
    }
}
