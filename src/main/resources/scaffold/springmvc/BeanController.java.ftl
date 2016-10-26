package ${controllerPackage};

import javax.validation.Valid;

import ${entityPackage}.${entityName};
import ${repositoryPackage}.${entityName}Repository;
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
@RequestMapping(value = "/${entity}")
public class ${entityName}Controller {
	
	@Autowired
	private ${entityName}Repository ${entity}Repository;
	
	@RequestMapping(value = "/${entity}-list", method = RequestMethod.GET)
	public String view(Model model, Pageable pageable){
		Page<${entityName}> ${entity}s = get${entityName}Repository().findAll(pageable);
		model.addAttribute("items", ${entity}s.getContent());
		model.addAttribute("hasPrevious", ${entity}s.getNumber() > 0);
		model.addAttribute("hasNext", ${entity}s.getNumber() < ${entity}s.getTotalPages() - 1);
		model.addAttribute("currentPage", ${entity}s.getNumber());
		return "/${entity}/${entity}-list.jsp";
	}
	
	@RequestMapping(value = "/${entity}-save", method = RequestMethod.POST)
	public String add(@Valid final ${entityName} ${entity}, BindingResult bindingResult){
		if(bindingResult.hasErrors()){
			return "/${entity}/${entity}-form.jsp";
		}
		
		this.get${entityName}Repository().save(${entity});
		return "redirect:/${entity}/${entity}-list";
	}
	
	@RequestMapping(value = "/${entity}-add", method = RequestMethod.GET)
	public String edit(Model model){
		model.addAttribute("${entity}", new ${entityName}());
		return "/${entity}/${entity}-form.jsp";
	}
	
	@RequestMapping(value = "/${entity}-edit/{itemId}", method = RequestMethod.GET)
	public String edit(@PathVariable("itemId") Long itemId, Model model){
		${entityName} ${entity} = get${entityName}Repository().findOne(itemId);
		model.addAttribute("${entity}", ${entity});
		return "/${entity}/${entity}-form.jsp";
	}
	
	@RequestMapping(value = "/${entity}-remove/{id}", method = RequestMethod.GET)
	public String remove(@PathVariable("id") Long id, Model model) {
        get${entityName}Repository().delete(id);
        return "redirect:/${entity}/${entity}-list";
	}

	public ${entityName}Repository get${entityName}Repository() {
		return ${entity}Repository;
	}

	public void set${entityName}Repository(${entityName}Repository ${entity}Repository) {
		this.${entity}Repository = ${entity}Repository;
	}
}
