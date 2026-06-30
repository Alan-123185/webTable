package com.xushu.webtable.utils;

public class CurrentHolder {
    private static ThreadLocal<Integer> holder=new ThreadLocal<>();
    private static ThreadLocal<Integer> roleHolder=new ThreadLocal<>();
    public static void set(Integer id){
        holder.set(id);
    }
    public static Integer get(){
        return holder.get();
    }
    public static void remove(){
         holder.remove();
         roleHolder.remove();
    }

    public static void setRole(Integer role){
        roleHolder.set(role);
    }
    public static Integer getRole(){
        return roleHolder.get();
    }
}
