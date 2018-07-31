require 'spec_helper'

feature 'Passwords sign-in, sign-out, session' do

  context 'an existing admin user ' do

    before :each do
      @user = FactoryBot.create :admin
    end

    scenario 'signing in with the wrong password does not work' do

      visit '/'
      click_on 'Sign in with password'
      fill_in 'email', with: @user.email
      fill_in 'password', with: 'bogus'
      click_on 'Sign in'

      # we are not signed-in
      expect(page).not_to have_content @user.email

    end

    scenario 'signing in with the correct password does work ' \
      'and we can see that we use session authentication ' do

      visit '/'
      click_on 'Sign in with password'
      fill_in 'email', with: @user.email
      fill_in 'password', with: @user.password
      click_on 'Sign in'

      expect(page).to have_content @user.email


      # the authentication method is session
      visit '/auth/info'

      wait_until {page.has_content? /authentication-method.+session/}

    end

    scenario 'signing out after signing in does work 'do

      visit '/'
      click_on 'Sign in with password'

      fill_in 'email', with: @user.email
      fill_in 'password', with: @user.password
      click_on 'Sign in'

      expect(page).to have_content @user.email

      click_on 'Sign out'

      # we are signed-out
      expect(page).not_to have_content @user.email


    end

  end
end
