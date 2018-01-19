require 'spec_helper'
require 'pry'

feature 'Initial admin', type: :feature do
  scenario 'Create an initial admin and sign works ' do
    visit '/'

    # we get redirected to the initial admin because there are no admins yet
    expect(page).to have_content  "Initial Admin"

    # we create the initial admin
    fill_in 'email', with: 'admin@example.com'
    fill_in 'password', with: 'password'
    click_on 'Create'

    # we sign-in as the admin
    fill_in 'email', with: 'admin@example.com'
    fill_in 'password', with: 'password'
    click_on 'Sign in'

    # we are signed-in
    expect(page).to have_content 'admin@example.com'

    # the authentication method is session
    visit '/auth'

    wait_until {page.has_content? /authentication-method.+session/}

    click_on 'Sign out'

    # we are signed-out
    expect(page).not_to have_content 'admin@example.com'


  end
end
