package controllers;

import play.mvc.With;

import models.Tag;

@With(Secure.class)
public class Tags extends CRUD {

}
