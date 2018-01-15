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
      expect(first('.user-nav img.user-img-32')).not_to be

    end

    scenario 'signing in with the correct password does work ' \
      'and we can see that we use session authentication ' do

      visit '/'

      fill_in 'email', with: @user.email
      fill_in 'password', with: @user.password
      click_on 'Sign in'

      expect(first('.user-nav img.user-img-32')).to be

      # the authentication method is session
      visit '/auth'

      wait_until {page.has_content? /authentication-method.+session/}

    end


    scenario 'signing out after signing in does work 'do

      visit '/'

      fill_in 'email', with: @user.email
      fill_in 'password', with: @user.password
      click_on 'Sign in'

      expect(first('.user-nav img.user-img-32')).to be

      find("#sign-out").click

      # we are signed-out
      expect(first('.user-nav img.user-img-32')).not_to be


    end
  end
end
