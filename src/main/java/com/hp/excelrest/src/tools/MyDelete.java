package com.hp.excelrest.src.tools;

import org.apache.http.client.methods.HttpPost;

class MyDelete extends HttpPost{
    public MyDelete(String url){
        super(url);
    }
    @Override
    public String getMethod() {
        return "DELETE";
    }
}