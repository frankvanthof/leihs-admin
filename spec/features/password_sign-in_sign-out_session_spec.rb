require 'spec_helper'

feature 'Passwords sign-in, sign-out, session' do

  context 'an existing admin user ' do

    before :each do
      @user = FactoryBot.create :admin
    end

    scenario 'signing in with the wrong password '\
      ' does not work and shows an sing-in warning 'do

      visit '/'

      fill_in 'email', with: @user.email
      fill_in 'password', with: 'bogus'
      click_on 'Sign in'

      expect(page).to have_content \
        "Make sure that you use the correct email-address and the correct password"

      # we are not signed-in
      expect(page).not_to have_content @user.email


    end

    scenario 'signing in with the correct password does work ' \
      'and we can see that we use session authentication ' do

      visit '/'

      fill_in 'email', with: @user.email
      fill_in 'password', with: @user.password
      click_on 'Sign in'

      expect(page).to have_content @user.email


      # the authentication method is session
      visit '/auth'

      wait_until {page.has_content? /authentication-method.+session/}

    end


    scenario 'signing out after signing in does work 'do

      visit '/'

      fill_in 'email', with: @user.email
      fill_in 'password', with: @user.password
      click_on 'Sign in'

      expect(page).to have_content @user.email

      click_on 'Sign out'

      # we are signed-out
      expect(page).not_to have_content @user.email


    end


    scenario 'changing the password as an admin does work' do

      visit '/'

      fill_in 'email', with: @user.email
      fill_in 'password', with: @user.password
      click_on 'Sign in'

      # set a new password in the user's edit page
      click_on @user.email
      click_on 'Edit'
      new_password = Faker::Internet.password(10, 20, true, true)

      fill_in 'password', with: new_password
      click_on 'Save'
      wait_until { not first('.modal')}

      # sign in with the new password does work
      click_on 'Sign out'
      visit '/'
      fill_in 'email', with: @user.email
      fill_in 'password', with: new_password
      click_on 'Sign in'
      expect(page).to have_content @user.email

    end


    scenario 'disabling sign_in_allowed immediatelly resets the session and prevents sign-in' do

      visit '/'

      fill_in 'email', with: @user.email
      fill_in 'password', with: @user.password
      click_on 'Sign in'

      # set a new password in the user's edit page
      click_on @user.email
      click_on 'Edit'
      uncheck 'sign_in_enabled'
      click_on 'Save'
      wait_until { first('.modal', text: 'OK') }

      sleep 1
      visit '/'

      # we are signed-out
      expect(page).not_to have_content @user.email

      # we can not sign in
      fill_in 'email', with: @user.email
      fill_in 'password', with: @user.password
      click_on 'Sign in'

      expect(page).not_to have_content @user.email
      expect(page).to have_content \
        'contact your leihs administrator if sign-in fails persistently'

    end

    scenario 'disabling password_sign_in_allowed prevents password sign-in' do

      visit '/'

      fill_in 'email', with: @user.email
      fill_in 'password', with: @user.password
      click_on 'Sign in'

      # set a new password in the user's edit page
      click_on @user.email
      click_on 'Edit'
      uncheck 'password_sign_in_enabled'
      click_on 'Save'
      wait_until { first('.modal', text: 'OK') }

      sleep 1

      visit '/'
      click_on 'Sign out'


      # we are signed-out
      expect(page).not_to have_content @user.email

      # we can not sign in
      fill_in 'email', with: @user.email
      fill_in 'password', with: @user.password
      click_on 'Sign in'

      expect(page).not_to have_content @user.email
      expect(page).to have_content \
        'contact your leihs administrator if sign-in fails persistently'

    end

  end
end
