package org.delme.controller;

import javax.transaction.Transactional;
import javax.validation.Valid;

import org.delme.model.User;
import org.delme.repository.UserRepository;
import org.hibernate.service.spi.InjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
@RequestMapping(value = "/user")
public class UserController {
	
	@Autowired
	private UserRepository userRepository;
	
	@RequestMapping(value = "/user-list", method = RequestMethod.GET)
	public String view(Model model, Pageable pageable){
		Page<User> users = getUserRepository().findAll(pageable);
		model.addAttribute("users", users.getContent());
		model.addAttribute("hasPrevious", users.getNumber() > 0);
		model.addAttribute("hasNext", users.getNumber() < users.getTotalPages() - 1);
		model.addAttribute("currentPage", users.getNumber());
		return "/user/user-list.jsp";
	}
	
	@RequestMapping(value = "/user-save", method = RequestMethod.POST)
	public String add(@Valid final User user, BindingResult bindingResult){
		if(bindingResult.hasErrors()){
			return "/user/user-form.jsp";
		}
		
		this.getUserRepository().save(user);
		return "redirect:/user/user-list";
	}
	
	@RequestMapping(value = "/user-add", method = RequestMethod.GET)
	public String edit(Model model){
		model.addAttribute("user", new User());
		return "/user/user-form.jsp";
	}
	
	@RequestMapping(value = "/user-edit/{itemId}", method = RequestMethod.GET)
	public String edit(@PathVariable("itemId") Long itemId, Model model){
		User user = getUserRepository().findOne(itemId);
		model.addAttribute("user", user);
		return "/user/user-form.jsp";
	}
	
	@RequestMapping(value = "/user-remove/{id}", method = RequestMethod.GET)
	public String remove(@PathVariable("id") Long id, Model model) {
        getUserRepository().delete(id);
        return "redirect:/user/user-list";
	}

	public UserRepository getUserRepository() {
		return userRepository;
	}

	public void setUserRepository(UserRepository userRepository) {
		this.userRepository = userRepository;
	}
}
