import logging
from django.http import HttpResponse
from django.shortcuts import render_to_response,redirect
from django.contrib.auth.decorators import login_required
from django.template import RequestContext
from django.utils.translation import ugettext as _

from tmapp.forms import TranslationInputForm

import controller

logger = logging.getLogger(__name__)

TRAINING_BUTTON_TEXT = [_('Next: Browser Check'),
                        _('Next: Experiment Goals'),
                        _('Next: Experiment Description'),
                        _('Next: Job Description'),
                        _('Next: Interface Descriptions'),
                        _('Next: Interface Tutorial'),
                        _('Next: Open the practice UI'),
                        _('Try another document')]
                        

##
## Notes:
##  * RuntimeError raises Http500
##  * Raise Http404 for invalid requests

@login_required
def index(request):
    """
    Return the main index template.
    """
    status = controller.get_user_app_status(request.user)
    return render_to_response('index.html',
                              {'page_title' : 'Overview',
                               'status' : status},
                              context_instance=RequestContext(request))

@login_required
def training(request, step_id=None):
    page_title = _('Experiment Overview and CAT Training')
    page_name = _('Experiment Overview and Training')
    
    if request.method == 'GET':
        done_training = controller.user_training_status(request.user)
        src_lang,tgt_lang = controller.get_user_translation_direction(request.user)
        step_id = int(step_id) + 1 if step_id else 0
        if step_id >= len(TRAINING_BUTTON_TEXT):
            raise Http404
        return render_to_response('training.html',
                                  {'step' : step_id,
                                   'page_title' : page_title,
                                   'page_name' : page_name,
                                   'src_lang' : src_lang,
                                   'tgt_lang' : tgt_lang,
                                   'form_action' : '/tm/training/ui/',
                                   'show_ui_link' : not done_training,
                                   'ui_link' : '/tm/training/ui/',
                                   'form_button_text' : TRAINING_BUTTON_TEXT[step_id]},
                                  context_instance=RequestContext(request))
    else:
        raise Http404

@login_required
def training_ui(request):
    is_training = True
    if request.method == 'GET':
        conf,form = controller.get_translate_configuration_for_user(request.user,is_training)
        if conf:
            return render_to_response('translate.html',
                                      {'conf' : conf,
                                       'form_action' : '/tm/training/ui/',
                                       'form' : form,
                                       'form_button_text' : 'Go to next training document',
                                       'training' : True },
                                      context_instance=RequestContext(request))
        else:
            controller.user_training_status(request.user, True)
            return redirect('/tm/')
        
    elif request.method == 'POST':
        # Next document
        controller.save_translation_session(request.user, request.POST, is_training)
        return redirect('/tm/training/ui/')

@login_required
def translate(request):
    """
    Return the translation UI and static content.
    """
    # TODO(spenceg): Need to change the form action per Jason's client
    #                -side manipulation of the URL?
    if request.method == 'GET':
        conf,form = controller.get_translate_configuration_for_user(request.user)
        if conf:
            return render_to_response('translate.html',
                                      {'conf' : conf,
                                       'form_action' : '/tm/translate/',
                                       'form' : form,
                                       'form_button_text' : 'Submit translations'},
                                      context_instance=RequestContext(request))
        else:
            # No more translation sessions
            return redirect('/tm/')
    elif request.method == 'POST':
        # Note: raises runtime error if the form doesn't validate
        # Then what do we do?
        controller.save_translation_session(request.user, request.POST)
        # Go to next document
        return redirect('/tm/translate/')

@login_required
def form_demographic(request):
    form = None
    form_instructions = _('Please complete this demographic survey. This information will remain confidential and will not be linked in any way with your real identity.')
    form_title = _('Demographic Survey')
    page_name = _('Demographic Survey')
    
    if request.method == 'GET':
        form = controller.get_demographic_form(request.user)

    elif request.method == 'POST':
        form = controller.get_demographic_form(request.user, request.POST)
        if form.is_valid():
            controller.save_modelform(request.user, form)
            return redirect('/tm/')
    else:
        # TODO log message
        raise Http404

    return render_to_response('survey.html',
                              {'form':form,
                               'form_instructions' : form_instructions,
                               'form_title' : form_title,
                               'page_name' : page_name},
                              context_instance=RequestContext(request))        
@login_required
def form_exit(request):
    form = None
    form_instructions = _('Please fill out this survey about your experience with the different CAT interfaces and modes of assistance.')
    form_title = _('Exit Questionnaire')
    page_name = _('Exit Questionnaire')
    
    if request.method == 'GET':
        form = controller.get_exit_form(request.user)

    elif request.method == 'POST':
        form = controller.get_exit_form(request.user,request.POST)
        if form.is_valid():
            controller.save_modelform(request.user, form)
            return redirect('/tm/')
    else:
        # TODO log message
        raise Http404

    return render_to_response('survey.html',
                              {'form':form,
                               'form_instructions' : form_instructions,
                               'form_title' : form_title,
                               'page_name' : page_name},
                              context_instance=RequestContext(request))   
